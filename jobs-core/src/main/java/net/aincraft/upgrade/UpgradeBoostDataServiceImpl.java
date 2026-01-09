package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostSource;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableType;
import net.aincraft.container.PayableTypes;
import net.aincraft.registry.Registry;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of UpgradeBoostDataService.
 * Aggregates boost sources from unlocked upgrade nodes using the composition API.
 */
@Singleton
public final class UpgradeBoostDataServiceImpl implements UpgradeBoostDataService {

  private final PlayerUpgradeRepository upgradeRepository;
  private final Registry<UpgradeTree> treeRegistry;

  @Inject
  public UpgradeBoostDataServiceImpl(
      @NotNull PlayerUpgradeRepository upgradeRepository,
      @NotNull Registry<UpgradeTree> treeRegistry
  ) {
    this.upgradeRepository = upgradeRepository;
    this.treeRegistry = treeRegistry;
  }

  @Override
  public List<BoostSource> getBoostSources(@NotNull UUID playerId, @NotNull Key jobKey) {
    String playerIdStr = playerId.toString();
    String jobKeyStr = jobKey.value();

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
