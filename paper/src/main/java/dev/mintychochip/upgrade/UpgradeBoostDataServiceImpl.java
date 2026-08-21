package dev.mintychochip.upgrade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.container.PayableTypes;
import dev.mintychochip.registry.Registry;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of UpgradeBoostDataService.
 * Aggregates boost sources from unlocked upgrade nodes using the composition API.
 * When a v2 {@link SkillTreeState} and matching {@link SkillTree} exist, the state
 * is the source of truth; otherwise the legacy {@link UpgradeTree} path is used.
 */
public final class UpgradeBoostDataServiceImpl implements UpgradeBoostDataService {

  private final PlayerUpgradeRepository upgradeRepository;
  private final Registry<UpgradeTree> treeRegistry;
  private final Registry<SkillTree> skillTreeRegistry;

  public UpgradeBoostDataServiceImpl(
      @NotNull PlayerUpgradeRepository upgradeRepository,
      @NotNull Registry<UpgradeTree> treeRegistry,
      @NotNull Registry<SkillTree> skillTreeRegistry
  ) {
    this.upgradeRepository = upgradeRepository;
    this.treeRegistry = treeRegistry;
    this.skillTreeRegistry = skillTreeRegistry;
  }

  @Override
  public @NotNull List<BoostSource> getBoostSources(@NotNull UUID playerId, @NotNull Key jobKey) {
    String playerIdStr = playerId.toString();
    String jobKeyStr = jobKey.value();

    // v2 state is the source of truth for boost lookup when a v2 tree/state exists.
    SkillTreeState state = upgradeRepository.loadState(playerIdStr, jobKeyStr);
    Optional<SkillTree> skillTreeOpt = skillTreeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(jobKeyStr))
        .findFirst();
    if (state != null && skillTreeOpt.isPresent()) {
      return buildBoostSourcesForState(state, skillTreeOpt.get());
    }

    // Fall back to the legacy path when no v2 state/tree is available.
    return buildLegacyBoostSources(playerIdStr, jobKeyStr, jobKey);
  }

  /**
   * Derive boost sources from a player's v2 skill tree state. Effects come from
   * {@link SkillNode#activeEffects(int)} per owned node level so cumulative and
   * replace level modes are respected. Non-boost effects are ignored.
   *
   * @param state the player's skill tree state
   * @param tree  the matching skill tree
   * @return boost sources for the state's active boost effects, in stable node order
   */
  public static @NotNull List<BoostSource> buildBoostSourcesForState(
      @NotNull SkillTreeState state,
      @NotNull SkillTree tree
  ) {
    List<BoostSource> result = new ArrayList<>();
    List<Map.Entry<String, Integer>> owned = state.nodeLevels().entrySet().stream()
        .filter(entry -> entry.getValue() > 0)
        .sorted(Map.Entry.comparingByKey())
        .toList();

    for (Map.Entry<String, Integer> entry : owned) {
      Optional<SkillNode> nodeOpt = tree.node(entry.getKey());
      if (nodeOpt.isEmpty()) {
        continue;
      }
      SkillNode node = nodeOpt.get();
      List<NodeEffect> activeEffects = node.activeEffects(entry.getValue());
      for (int effectIndex = 0; effectIndex < activeEffects.size(); effectIndex++) {
        NodeEffect effect = activeEffects.get(effectIndex);
        BoostSource source = buildBoostSource(node, effect, tree.jobKey(), effectIndex);
        if (source != null) {
          result.add(source);
        }
      }
    }

    return List.copyOf(result);
  }

  private List<BoostSource> buildLegacyBoostSources(
      @NotNull String playerIdStr,
      @NotNull String jobKeyStr,
      @NotNull Key jobKey
  ) {
    PlayerUpgradeDataImpl playerData = upgradeRepository.loadPlayerData(playerIdStr, jobKeyStr);
    if (playerData == null) {
      return List.of();
    }

    Set<String> unlockedNodes = playerData.unlockedNodes();
    if (unlockedNodes.isEmpty()) {
      return List.of();
    }

    Optional<UpgradeTree> treeOpt = treeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(jobKeyStr))
        .findFirst();

    if (treeOpt.isEmpty()) {
      return List.of();
    }

    UpgradeTree tree = treeOpt.get();

    // Filter nodes based on perk policies
    Map<String, UpgradeNode> activeNodes = new HashMap<>();

    for (String nodeKey : unlockedNodes) {
      var nodeOpt = tree.getNode(nodeKey);
      if (nodeOpt.isEmpty()) {
        continue;
      }
      UpgradeNode node = nodeOpt.get();
      PerkPolicy policy = tree.getPerkPolicy(node.perkId());

      if (policy == PerkPolicy.MAX) {
        UpgradeNode existing = activeNodes.get(node.perkId());
        if (existing == null || node.level() > existing.level()) {
          activeNodes.put(node.perkId(), node);
        }
      } else {
        activeNodes.put(node.key().asString(), node);
      }
    }

    // Build BoostSource instances from active nodes
    List<BoostSource> result = new ArrayList<>();

    for (UpgradeNode node : activeNodes.values()) {
      for (UpgradeEffect effect : node.effects()) {
        BoostSource source = buildBoostSource(node, effect, jobKey);
        if (source != null) {
          result.add(source);
        }
      }
    }

    return result;
  }

  @Nullable
  private static BoostSource buildBoostSource(
      SkillNode node, NodeEffect effect, String jobKey, int effectIndex
  ) {
    if (effect instanceof NodeEffect.RuledBoostEffect ruled) {
      // Use the full BoostSource from the effect - already has conditions/rules
      return ruled.boostSource();
    }

    if (effect instanceof NodeEffect.BoostEffect simple) {
      // Wrap the simple boost in a BoostSource with always-true condition.
      // The active-effect index keeps multiple effects of one node (cumulative
      // levels, multi-effect nodes) on distinct source keys so BoostEngine
      // aggregation does not overwrite earlier sources.
      return new SimpleUpgradeBoostSource(
          Key.key("modularjobs",
              "upgrade/" + jobKey + "/" + node.key().value() + "/effect-" + effectIndex),
          simple.target(),
          simple.multiplier(),
          node.name()
      );
    }

    return null;
  }

  @Nullable
  private BoostSource buildBoostSource(UpgradeNode node, UpgradeEffect effect, Key jobKey) {
    if (effect instanceof UpgradeEffect.RuledBoostEffect ruled) {
      // Use the full BoostSource from the effect - already has conditions/rules
      return ruled.boostSource();
    }

    if (effect instanceof UpgradeEffect.BoostEffect simple) {
      // Wrap legacy simple boost in a BoostSource with always-true condition
      return new SimpleUpgradeBoostSource(
          Key.key("modularjobs", "upgrade/" + jobKey.value() + "/" + node.perkId()),
          simple.target(),
          simple.multiplier(),
          node.name()
      );
    }

    return null;
  }

  /**
   * Simple BoostSource wrapper for legacy BoostEffect.
   * Applies boost if the payable target matches.
   */
  private record SimpleUpgradeBoostSource(
      Key key,
      String target,
      BigDecimal multiplier,
      String nodeName
  ) implements BoostSource {

    @Override
    public @NotNull Key key() {
      return key;
    }

    @Override
    public @NotNull List<Boost> evaluate(BoostContext context) {
      if (!appliesToPayable(context.payable())) {
        return List.of();
      }
      return List.of(amount -> amount.multiply(multiplier));
    }

    @Override
    public @Nullable String description() {
      return nodeName + " upgrade";
    }

    private boolean appliesToPayable(Payable payable) {
      if (UpgradeEffect.BoostEffect.TARGET_ALL.equals(target)) {
        return true;
      }
      PayableType payableType = payable.type();
      if (UpgradeEffect.BoostEffect.TARGET_XP.equals(target)) {
        return payableType == PayableTypes.EXPERIENCE;
      }
      if (UpgradeEffect.BoostEffect.TARGET_MONEY.equals(target)) {
        return payableType == PayableTypes.ECONOMY;
      }
      return false;
    }
  }
}
