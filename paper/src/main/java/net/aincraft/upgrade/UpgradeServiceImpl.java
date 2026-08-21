package net.aincraft.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.aincraft.JobProgression;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of UpgradeService.
 */
public final class UpgradeServiceImpl implements UpgradeService {

  private final Registry<UpgradeTree> treeRegistry;
  private final Registry<SkillTree> skillTreeRegistry;
  private final PlayerUpgradeRepository repository;
  private final JobService jobService;
  private final UpgradeEffectApplier effectApplier;

  // In-memory cache: playerId -> jobKey -> data
  private final Map<String, Map<String, PlayerUpgradeDataImpl>> cache = new ConcurrentHashMap<>();

  public UpgradeServiceImpl(
      Registry<UpgradeTree> treeRegistry,
      Registry<SkillTree> skillTreeRegistry,
      PlayerUpgradeRepository repository,
      JobService jobService,
      UpgradeEffectApplier effectApplier
  ) {
    this.treeRegistry = treeRegistry;
    this.skillTreeRegistry = skillTreeRegistry;
    this.repository = repository;
    this.jobService = jobService;
    this.effectApplier = effectApplier;
  }

  @Override
  public @NotNull Optional<UpgradeTree> getTree(@NotNull String jobKey) {
    String plainJobKey = jobKey;
    if (jobKey.contains(":")) {
      plainJobKey = jobKey.substring(jobKey.indexOf(':') + 1);
    }

    final String finalPlainJobKey = plainJobKey;
    return treeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(finalPlainJobKey))
        .findFirst();
  }

  @Override
  public @NotNull Optional<SkillTree> getSkillTree(@NotNull String jobKey) {
    return skillTreeFor(jobKey);
  }

  @Override
  public @NotNull Collection<UpgradeTree> getAllTrees() {
    return treeRegistry.stream().toList();
  }

  @Override
  public @NotNull PlayerUpgradeData getPlayerData(@NotNull String playerId, @NotNull String jobKey) {
    return getOrLoadData(playerId, jobKey);
  }

  @Override
  public @NotNull Set<UpgradeNode> getAvailableNodes(@NotNull String playerId, @NotNull String jobKey) {
    Optional<UpgradeTree> treeOpt = getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return Set.of();
    }

    UpgradeTree tree = treeOpt.get();
    PlayerUpgradeData data = getPlayerData(playerId, jobKey);
    return tree.getAvailableNodes(data.unlockedNodes());
  }

  @Override
  public @NotNull UnlockResult unlock(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
    // v2 tree takes precedence for matching jobs; legacy trees fall back below.
    if (skillTreeFor(jobKey).isPresent()) {
      return switch (purchaseSkillLevel(playerId, jobKey, nodeKey)) {
        case PurchaseResult.Success success -> new UnlockResult.Success(
            legacyNode(nodeKey, success.node().name()), success.remainingPoints());
        case PurchaseResult.InsufficientPoints ip ->
            new UnlockResult.InsufficientPoints(ip.required(), ip.available());
        case PurchaseResult.RequirementsNotMet rn -> new UnlockResult.PrerequisitesNotMet(rn.unmet());
        case PurchaseResult.PrerequisitesNotMet pm -> new UnlockResult.PrerequisitesNotMet(pm.missing());
        case PurchaseResult.ExcludedByChoice ec -> new UnlockResult.ExcludedByChoice(ec.conflicting());
        case PurchaseResult.AlreadyOwned ao -> new UnlockResult.AlreadyUnlocked(ao.nodeKey());
        case PurchaseResult.NodeNotFound nf -> new UnlockResult.NodeNotFound(nf.nodeKey());
        case PurchaseResult.TreeNotFound tf -> new UnlockResult.TreeNotFound(tf.jobKey());
      };
    }

    // Get tree
    Optional<UpgradeTree> treeOpt = getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return new UnlockResult.TreeNotFound(jobKey);
    }
    UpgradeTree tree = treeOpt.get();

    // Get node
    Optional<UpgradeNode> nodeOpt = tree.getNode(nodeKey);
    if (nodeOpt.isEmpty()) {
      return new UnlockResult.NodeNotFound(nodeKey);
    }
    UpgradeNode node = nodeOpt.get();

    // Get player data
    PlayerUpgradeDataImpl data = getOrLoadData(playerId, jobKey);

    // Check if already unlocked
    if (data.hasUnlocked(nodeKey)) {
      return new UnlockResult.AlreadyUnlocked(nodeKey);
    }

    // Check cost
    int available = data.availableSkillPoints();
    if (node.cost() > available) {
      return new UnlockResult.InsufficientPoints(node.cost(), available);
    }

    // Check prerequisites
    Set<String> missingPrereqs = new HashSet<>();
    for (String prereq : node.prerequisites()) {
      if (!data.hasUnlocked(prereq)) {
        missingPrereqs.add(prereq);
      }
    }
    if (!missingPrereqs.isEmpty()) {
      return new UnlockResult.PrerequisitesNotMet(missingPrereqs);
    }

    // Check exclusives
    Set<String> conflicting = new HashSet<>();
    for (String exclusive : node.exclusive()) {
      if (data.hasUnlocked(exclusive)) {
        conflicting.add(exclusive);
      }
    }
    if (!conflicting.isEmpty()) {
      return new UnlockResult.ExcludedByChoice(conflicting);
    }

    // Unlock the node
    data.unlock(nodeKey);

    // Track perk level
    data.setPerkLevel(node.perkId(), node.level());

    // Apply effects if player is online
    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    if (player != null && player.isOnline()) {
      effectApplier.applyNodeEffects(player, node);
    }

    // Persist
    repository.savePlayerData(data);

    int remaining = data.availableSkillPoints();
    return new UnlockResult.Success(node, remaining);
  }

  @Override
  public void awardSkillPoints(@NotNull String playerId, @NotNull String jobKey, int points) {
    // v2 trees persist points in SkillTreeState; legacy trees keep the old data path.
    if (skillTreeFor(jobKey).isPresent()) {
      SkillTreeState state = loadOrCreateState(playerId, jobKey);
      SkillTreeState updated = new SkillTreeState(
          playerId, jobKey, state.totalSkillPoints() + points, state.nodeLevels(), state.state(),
          state.currentJobLevel(), state.permissionCheck());
      repository.saveState(updated);
      return;
    }
    PlayerUpgradeDataImpl data = getOrLoadData(playerId, jobKey);
    data.addSkillPoints(points);
    repository.savePlayerData(data);
  }

  @Override
  public boolean resetUpgrades(@NotNull String playerId, @NotNull String jobKey) {
    // v2 trees refund skill levels through resetTree; legacy trees keep the old path.
    if (skillTreeFor(jobKey).isPresent()) {
      return resetTree(playerId, jobKey);
    }
    PlayerUpgradeDataImpl data = getOrLoadData(playerId, jobKey);

    // Get player if online for effect unapplication
    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);

    // Get tree for node lookup
    Optional<UpgradeTree> treeOpt = getTree(jobKey);

    // Clear all unlocks but keep total skill points
    Set<String> unlocked = new HashSet<>(data.unlockedNodes());
    for (String nodeKey : unlocked) {
      // Unapply effects before locking
      if (player != null && player.isOnline() && treeOpt.isPresent()) {
        treeOpt.get().getNode(nodeKey).ifPresent(node ->
            effectApplier.unapplyNodeEffects(player, node)
        );
      }

      data.lock(nodeKey);
    }

    repository.savePlayerData(data);
    return true;
  }

  @Override
  public @NotNull SkillTreeState getSkillTreeState(
      @NotNull String playerId, @NotNull String jobKey) {
    return loadOrCreateState(playerId, jobKey);
  }

  @Override
  public @NotNull PurchaseResult purchaseSkillLevel(
      @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
    Optional<SkillTree> treeOpt = skillTreeFor(jobKey);
    if (treeOpt.isEmpty()) {
      return new PurchaseResult.TreeNotFound(jobKey);
    }
    SkillTree tree = treeOpt.get();
    Optional<SkillNode> nodeOpt = tree.node(nodeKey);
    if (nodeOpt.isEmpty()) {
      return new PurchaseResult.NodeNotFound(nodeKey);
    }
    SkillNode node = nodeOpt.get();
    if (node.isMajor()) {
      // Majors are one-time choices with state writes; route through
      // purchaseMajor so NodeStateWrite.SET values are applied exactly once
      // and never mutated through the per-level path.
      return purchaseMajor(playerId, jobKey, nodeKey);
    }
    SkillTreeState state = loadOrCreateState(playerId, jobKey);

    if (state.levelOf(nodeKey) >= node.maxLevel()) {
      return new PurchaseResult.AlreadyOwned(nodeKey);
    }

    int nextLevel = state.levelOf(nodeKey) + 1;
    final int cost = node.levelCost(nextLevel);

    Set<String> unmet = unmetRequirements(node, state);
    if (!unmet.isEmpty()) {
      return new PurchaseResult.RequirementsNotMet(unmet);
    }
    Set<String> missingPrereqs = missingPrerequisites(node, state);
    if (!missingPrereqs.isEmpty()) {
      return new PurchaseResult.PrerequisitesNotMet(missingPrereqs);
    }
    Set<String> conflicting = conflictingExcludes(tree, node, state);
    if (!conflicting.isEmpty()) {
      return new PurchaseResult.ExcludedByChoice(conflicting);
    }
    if (cost > tree.availablePoints(state)) {
      return new PurchaseResult.InsufficientPoints(cost, tree.availablePoints(state));
    }

    Map<String, Integer> levels = new HashMap<>(state.nodeLevels());
    levels.put(nodeKey, nextLevel);
    SkillTreeState updated = new SkillTreeState(
        playerId, jobKey, state.totalSkillPoints(), levels, state.state(),
        state.currentJobLevel(), state.permissionCheck());
    repository.saveState(updated);
    syncEffectsFor(playerId, tree, state, updated);
    return new PurchaseResult.Success(node, tree.availablePoints(updated));
  }

  @Override
  public @NotNull PurchaseResult purchaseMajor(
      @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
    Optional<SkillTree> treeOpt = skillTreeFor(jobKey);
    if (treeOpt.isEmpty()) {
      return new PurchaseResult.TreeNotFound(jobKey);
    }
    SkillTree tree = treeOpt.get();
    Optional<SkillNode> nodeOpt = tree.node(nodeKey);
    if (nodeOpt.isEmpty()) {
      return new PurchaseResult.NodeNotFound(nodeKey);
    }
    SkillNode node = nodeOpt.get();
    SkillTreeState state = loadOrCreateState(playerId, jobKey);

    if (!node.isMajor()) {
      return new PurchaseResult.AlreadyOwned(nodeKey);
    }
    if (state.hasUnlocked(nodeKey)) {
      return new PurchaseResult.AlreadyOwned(nodeKey);
    }

    Set<String> unmet = unmetRequirements(node, state);
    if (!unmet.isEmpty()) {
      return new PurchaseResult.RequirementsNotMet(unmet);
    }
    Set<String> missingPrereqs = missingPrerequisites(node, state);
    if (!missingPrereqs.isEmpty()) {
      return new PurchaseResult.PrerequisitesNotMet(missingPrereqs);
    }
    Set<String> conflicting = conflictingExcludes(tree, node, state);
    if (!conflicting.isEmpty()) {
      return new PurchaseResult.ExcludedByChoice(conflicting);
    }
    if (node.cost() > tree.availablePoints(state)) {
      return new PurchaseResult.InsufficientPoints(node.cost(), tree.availablePoints(state));
    }

    Map<String, Integer> levels = new HashMap<>(state.nodeLevels());
    levels.put(nodeKey, 1);
    Map<Key, String> nextState = new HashMap<>(state.state());
    // Both the "state" write list and the "state_set" effect vocabulary mutate
    // the derived state map; a major declared with either must behave the same.
    for (NodeStateWrite write : node.stateWrites()) {
      if (write.op() == NodeStateWrite.Op.SET) {
        nextState.put(write.key(), write.value());
      } else if (write.op() == NodeStateWrite.Op.REMOVE) {
        nextState.remove(write.key());
      }
    }
    for (NodeEffect effect : node.effects()) {
      if (effect instanceof NodeEffect.StateSetEffect stateSet) {
        if (stateSet.remove()) {
          nextState.remove(stateSet.key());
        } else {
          nextState.put(stateSet.key(), stateSet.value());
        }
      }
    }
    SkillTreeState updated = new SkillTreeState(
        playerId, jobKey, state.totalSkillPoints(), levels, nextState,
        state.currentJobLevel(), state.permissionCheck());
    repository.saveState(updated);
    syncEffectsFor(playerId, tree, state, updated);
    return new PurchaseResult.Success(node, tree.availablePoints(updated));
  }

  @Override
  public boolean resetTree(@NotNull String playerId, @NotNull String jobKey) {
    Optional<SkillTree> treeOpt = skillTreeFor(jobKey);
    if (treeOpt.isEmpty()) {
      return false;
    }
    SkillTree tree = treeOpt.get();
    SkillTreeState state = loadOrCreateState(playerId, jobKey);

    // Refund ordinary SKILL levels; preserve ROOT and MAJOR levels and state.
    Map<String, Integer> levels = new HashMap<>();
    for (String ownedKey : state.nodeLevels().keySet()) {
      tree.node(ownedKey).ifPresent(node -> {
        if (!node.isSkill()) {
          levels.put(ownedKey, 1);
        }
      });
    }
    SkillTreeState refunded = new SkillTreeState(
        playerId, jobKey, state.totalSkillPoints(), levels, state.state(),
        state.currentJobLevel(), state.permissionCheck());
    repository.saveState(refunded);
    syncEffectsFor(playerId, tree, state, refunded);
    return true;
  }

  @Override
  public void clearTreeState(@NotNull String playerId, @NotNull String jobKey) {
    // Revoke this tree's effects before the persisted state (the source of
    // truth) is deleted. The revocation diffs across ALL v2 trees: a
    // permission still derived by another tree the player owns survives
    // (same shared-permission guarantee as syncEffectsFor). Nothing is granted
    // here because the post-leave union is a subset of the pre-leave union.
    Player player = Bukkit.getPlayer(UUID.fromString(playerId));
    SkillTree departing = skillTreeFor(jobKey).orElse(null);
    if (player != null && player.isOnline() && departing != null) {
      Map<SkillTree, SkillTreeState> previousByTree = new HashMap<>();
      Map<SkillTree, SkillTreeState> currentByTree = new HashMap<>();
      for (SkillTree tree : skillTreeRegistry.stream().toList()) {
        SkillTreeState state = loadOrCreateState(playerId, tree.jobKey());
        previousByTree.put(tree, state);
        if (!tree.equals(departing)) {
          currentByTree.put(tree, state);
        }
      }
      effectApplier.syncEffects(player, previousByTree, currentByTree);
    }
    cache.computeIfPresent(playerId, (id, byJob) -> {
      byJob.remove(jobKey);
      return byJob;
    });
    repository.deletePlayerData(playerId, jobKey);
  }

  /**
   * Single v2 mutation sync path: diff the pre-mutation state against the
   * persisted state for an online player only. The previous state snapshot
   * lives only for this call — no persistent effect snapshot. The previous
   * and current maps cover EVERY registered v2 tree (the mutated tree's
   * captured pre/post snapshots replace its loaded state), so a permission
   * still derived by another active tree is never revoked. Unmocked servers
   * (tests) and offline players are no-ops.
   */
  private void syncEffectsFor(
      String playerId, SkillTree tree, SkillTreeState previous, SkillTreeState current) {
    // Bukkit.getPlayer requires a registered server; without one (standalone
    // tests, startup) there is nothing to sync.
    Player player;
    try {
      player = Bukkit.getPlayer(UUID.fromString(playerId));
    } catch (IllegalArgumentException e) {
      return;
    }
    if (player == null || !player.isOnline()) {
      return;
    }
    Map<SkillTree, SkillTreeState> previousByTree = new HashMap<>();
    Map<SkillTree, SkillTreeState> currentByTree = new HashMap<>();
    for (SkillTree registered : skillTreeRegistry.stream().toList()) {
      SkillTreeState state = loadOrCreateState(playerId, registered.jobKey());
      previousByTree.put(registered, state);
      currentByTree.put(registered, state);
    }
    // The loop above loads the already-persisted (mutated) state for this
    // tree; replace it with the exact captured pre/post snapshots.
    previousByTree.put(tree, previous);
    currentByTree.put(tree, current);
    try {
      effectApplier.syncEffects(player, previousByTree, currentByTree);
    } catch (IllegalArgumentException | IllegalStateException e) {
      // The purchase is already persisted; a sync failure must not roll it
      // back or surface a false failure (retry would double-spend). It is
      // logged instead of silently swallowed.
      Logger.getLogger(UpgradeServiceImpl.class.getName())
          .log(Level.WARNING, "Failed to sync upgrade effects for " + playerId
              + "/" + tree.jobKey(), e);
    }
  }

  private SkillTreeState loadOrCreateState(String playerId, String jobKey) {
    SkillTreeState loaded = repository.loadState(playerId, jobKey);
    if (loaded != null) {
      // Repository state carries default hooks; rebind runtime job level and
      // permission suppliers so persisted states evaluate requirements live.
      SkillTreeState rebound = bindRuntimeHooks(playerId, jobKey, loaded);
      return skillTreeFor(jobKey)
          .map(tree -> PlayerUpgradeRepository.hydrate(tree,
              remapLegacyNodeKeys(playerId, jobKey, rebound, tree)))
          .orElse(rebound);
    }
    return skillTreeFor(jobKey)
        .map(tree -> new SkillTreeState(
            playerId, jobKey, 0, Map.of(), Map.of(),
            currentJobLevel(playerId, jobKey), permissionCheck(playerId)))
        .orElseGet(() -> SkillTreeState.empty(playerId, jobKey));
  }

  /**
   * Migrates pre-v2 persisted node IDs to their converted perk keys when this
   * job's v2 tree derives from a legacy {@link UpgradeTree}. Without this the
   * first v2 write would persist raw legacy IDs into {@code node_levels} and
   * clear {@code unlocked_nodes}, stranding old progression permanently:
   * boost derivation, GUI ownership, and purchase gates would all miss it.
   * Legacy `_N`-suffixed IDs carry their level, so levels aggregate onto the
   * perk ({@code efficiency_1} + {@code efficiency_2} -> {@code efficiency}
   * at the max seen). Keys already known to the v2 tree pass through; unknown
   * keys are left untouched for diagnostics.
   */
  private SkillTreeState remapLegacyNodeKeys(
      String playerId, String jobKey, SkillTreeState state, SkillTree tree) {
    Optional<UpgradeTree> legacyOpt = getTree(jobKey);
    if (legacyOpt.isEmpty()) {
      return state; // Pure v2 tree; nothing aliases.
    }
    Map<String, String> perkByLegacyKey = new HashMap<>();
    Map<String, Integer> levelByLegacyKey = new HashMap<>();
    for (UpgradeNode node : legacyOpt.get().allNodes()) {
      String legacyKey = node.key().value();
      String perkId = node.perkId();
      perkByLegacyKey.put(legacyKey, perkId == null || perkId.isBlank() ? legacyKey : perkId);
      levelByLegacyKey.put(legacyKey, node.level());
    }
    Map<String, Integer> remapped = new HashMap<>(state.nodeLevels());
    boolean changed = false;
    for (Map.Entry<String, Integer> entry : new ArrayList<>(state.nodeLevels().entrySet())) {
      if (tree.node(entry.getKey()).isPresent()) {
        continue; // Already a v2 key.
      }
      String perk = perkByLegacyKey.get(entry.getKey());
      if (perk == null || tree.node(perk).isEmpty()) {
        continue; // Unknown to both trees; keep for diagnostics.
      }
      remapped.merge(perk,
          Math.max(entry.getValue(), levelByLegacyKey.getOrDefault(entry.getKey(), 1)),
          Math::max);
      remapped.remove(entry.getKey());
      changed = true;
    }
    if (!changed) {
      return state;
    }
    return new SkillTreeState(
        playerId, jobKey, state.totalSkillPoints(), remapped, state.state(),
        state.currentJobLevel(), state.permissionCheck());
  }

  private SkillTreeState bindRuntimeHooks(
      String playerId, String jobKey, SkillTreeState state) {
    return new SkillTreeState(
        state.playerId(), state.jobKey(), state.totalSkillPoints(),
        state.nodeLevels(), state.state(),
        currentJobLevel(playerId, jobKey), permissionCheck(playerId));
  }

  /**
   * Runtime job-level supplier backed by JobService progression. Resolves to 0
   * when the player has no progression or the job service cannot answer, so
   * offline callers and tests stay safe.
   */
  private IntSupplier currentJobLevel(String playerId, String jobKey) {
    return () -> {
      try {
        JobProgression progression = jobService.getProgression(playerId, jobKey);
        return progression == null ? 0 : progression.level();
      } catch (IllegalArgumentException | IllegalStateException e) {
        return 0;
      }
    };
  }

  /**
   * Runtime permission check against the online Bukkit player. Offline players
   * and non-UUID callers (tests) resolve to false.
   */
  private Predicate<String> permissionCheck(String playerId) {
    return permission -> {
      try {
        Player player = Bukkit.getPlayer(UUID.fromString(playerId));
        return player != null && player.isOnline() && player.hasPermission(permission);
      } catch (IllegalArgumentException e) {
        return false;
      }
    };
  }

  private Optional<SkillTree> skillTreeFor(String jobKey) {
    String plainJobKey = jobKey;
    if (jobKey.contains(":")) {
      plainJobKey = jobKey.substring(jobKey.indexOf(':') + 1);
    }
    final String finalPlainJobKey = plainJobKey;
    return skillTreeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(finalPlainJobKey))
        .findFirst();
  }

  private Set<String> unmetRequirements(SkillNode node, SkillTreeState state) {
    Set<String> unmet = new HashSet<>();
    for (Requirement requirement : node.requirements()) {
      if (!requirement.satisfied(state)) {
        unmet.add(requirement.getClass().getSimpleName());
      }
    }
    return Set.copyOf(unmet);
  }

  private Set<String> missingPrerequisites(SkillNode node, SkillTreeState state) {
    Set<String> missing = new HashSet<>();
    for (String prereq : node.prerequisites()) {
      if (!state.hasUnlocked(prereq)) {
        missing.add(prereq);
      }
    }
    return Set.copyOf(missing);
  }

  private Set<String> conflictingExcludes(SkillTree tree, SkillNode node, SkillTreeState state) {
    Set<String> conflicting = new HashSet<>();
    for (String excluded : node.excludes()) {
      if (state.hasUnlocked(excluded)) {
        conflicting.add(excluded);
      }
    }
    for (Map.Entry<String, Integer> owned : state.nodeLevels().entrySet()) {
      if (owned.getValue() > 0 && tree.symmetricExcludes(owned.getKey()).contains(node.key().value())) {
        conflicting.add(owned.getKey());
      }
    }
    return Set.copyOf(conflicting);
  }

  /** Legacy node view for v2 unlock results (name only; effects are synced in Task 7). */
  private @NotNull UpgradeNode legacyNode(String nodeKey, String nodeName) {
    return new UpgradeNode(
        Key.key("modularjobs", nodeKey), nodeName, null,
        "STONE", "STONE", null, null, 0,
        Set.of(), Set.of(), Set.of(), List.of(), List.of(), null, List.of(), "", 0);
  }

  private PlayerUpgradeDataImpl getOrLoadData(String playerId, String jobKey) {
    return cache
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .computeIfAbsent(jobKey, k -> loadOrCreate(playerId, jobKey));
  }

  private PlayerUpgradeDataImpl loadOrCreate(String playerId, String jobKey) {
    PlayerUpgradeDataImpl loaded = repository.loadPlayerData(playerId, jobKey);
    if (loaded != null) {
      skillTreeFor(jobKey).ifPresent(loaded::bindSkillTree);
      return loaded;
    }

    // Calculate retroactive skill points based on current job level
    int retroactiveSkillPoints = calculateRetroactiveSkillPoints(playerId, jobKey);

    if (retroactiveSkillPoints > 0) {
      // Create new data with calculated skill points
      PlayerUpgradeDataImpl newData = new PlayerUpgradeDataImpl(playerId, jobKey, retroactiveSkillPoints, Set.of());
      skillTreeFor(jobKey).ifPresent(newData::bindSkillTree);
      // Save to database immediately
      repository.savePlayerData(newData);
      return newData;
    }

    PlayerUpgradeDataImpl empty = PlayerUpgradeDataImpl.empty(playerId, jobKey);
    skillTreeFor(jobKey).ifPresent(empty::bindSkillTree);
    return empty;
  }

  /**
   * Calculate how many skill points a player should have based on their current job level.
   * This is used for retroactive skill point calculation when the upgrade system is first accessed.
   */
  private int calculateRetroactiveSkillPoints(String playerId, String jobKey) {
    // Check if this job has an upgrade tree
    Optional<UpgradeTree> treeOpt = getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return 0;
    }

    UpgradeTree tree = treeOpt.get();
    int skillPointsPerLevel = tree.skillPointsPerLevel();

    // Get player's current job progression
    try {
      JobProgression progression = jobService.getProgression(playerId, jobKey);
      if (progression == null) {
        return 0;
      }

      int currentLevel = progression.level();

      // Players start at level 1 with 0 XP, so they get skill points starting from level 1
      // If they're level 5, they should have: 5 * skillPointsPerLevel
      return currentLevel * skillPointsPerLevel;

    } catch (IllegalArgumentException e) {
      // Invalid UUID or job key
      return 0;
    }
  }
}
