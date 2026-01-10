package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.JobProgression;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of UpgradeService.
 */
@Singleton
public final class UpgradeServiceImpl implements UpgradeService {

  private final Registry<UpgradeTree> treeRegistry;
  private final PlayerUpgradeRepository repository;
  private final JobService jobService;
  private final UpgradeEffectApplier effectApplier;

  // In-memory cache: playerId -> jobKey -> data
  private final Map<String, Map<String, PlayerUpgradeDataImpl>> cache = new ConcurrentHashMap<>();

  @Inject
  public UpgradeServiceImpl(
      Registry<UpgradeTree> treeRegistry,
      PlayerUpgradeRepository repository,
      JobService jobService,
      UpgradeEffectApplier effectApplier
  ) {
    this.treeRegistry = treeRegistry;
    this.repository = repository;
    this.jobService = jobService;
    this.effectApplier = effectApplier;
  }

  @Override
  public @NotNull Optional<UpgradeTree> getTree(@NotNull String jobKey) {
    // Extract the plain job key (without namespace) for matching
    String plainJobKey = jobKey;
    if (jobKey.contains(":")) {
      plainJobKey = jobKey.substring(jobKey.indexOf(':') + 1);
    }

    final String finalPlainJobKey = plainJobKey;

    // Debug: log available trees
    if (treeRegistry.stream().findAny().isEmpty()) {
      Bukkit.getLogger().warning("[UpgradeService] No trees registered in registry!");
    } else {
      treeRegistry.stream().forEach(tree ->
          Bukkit.getLogger().info("[UpgradeService] Available tree: jobKey=" + tree.jobKey())
      );
    }

    Bukkit.getLogger().info("[UpgradeService] Looking for tree with jobKey=" + finalPlainJobKey + " (from input=" + jobKey + ")");

    return treeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(finalPlainJobKey))
        .findFirst();
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

    // Check cost - use per-level cost for level 1 if available
    int unlockCost = node.isUpgradeable() ? node.getCostForLevel(1) : node.cost();
    int available = data.availableSkillPoints();
    if (unlockCost > available) {
      return new UnlockResult.InsufficientPoints(unlockCost, available);
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

    // Set initial node level for upgradeable nodes
    if (node.isUpgradeable()) {
      data.setNodeLevel(nodeKey, 1);
    }

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
    PlayerUpgradeDataImpl data = getOrLoadData(playerId, jobKey);
    data.addSkillPoints(points);
    repository.savePlayerData(data);
  }

  @Override
  public boolean resetUpgrades(@NotNull String playerId, @NotNull String jobKey) {
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
      data.removeNodeLevel(nodeKey); // Clear node level for upgradeable nodes
    }

    // Clear all perk levels
    for (String perkId : new HashSet<>(data.perkLevels().keySet())) {
      data.removePerkLevel(perkId);
    }

    repository.savePlayerData(data);
    return true;
  }

  @Override
  public @NotNull UnlockResult upgradeNode(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey) {
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

    // Check node is upgradeable
    if (!node.isUpgradeable()) {
      return new UnlockResult.AlreadyMaxLevel(nodeKey, 1);
    }

    // Get player data
    PlayerUpgradeDataImpl data = getOrLoadData(playerId, jobKey);

    // Check node is unlocked
    if (!data.hasUnlocked(nodeKey)) {
      return new UnlockResult.NodeNotUnlocked(nodeKey);
    }

    // Check current level
    int currentLevel = data.getNodeLevel(nodeKey);
    if (currentLevel >= node.maxLevel()) {
      return new UnlockResult.AlreadyMaxLevel(nodeKey, node.maxLevel());
    }

    // Get cost for next level
    int nextLevel = currentLevel + 1;
    int upgradeCost = node.getCostForLevel(nextLevel);

    // Check cost
    int available = data.availableSkillPoints();
    if (upgradeCost > available) {
      return new UnlockResult.InsufficientPoints(upgradeCost, available);
    }

    // Upgrade
    data.setNodeLevel(nodeKey, nextLevel);
    data.setPerkLevel(node.perkId(), nextLevel); // Update perk level for scaling

    // Persist
    repository.savePlayerData(data);

    int remaining = data.availableSkillPoints();
    return new UnlockResult.NodeUpgraded(nodeKey, nextLevel, node.maxLevel(), remaining);
  }

  private PlayerUpgradeDataImpl getOrLoadData(String playerId, String jobKey) {
    return cache
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .computeIfAbsent(jobKey, k -> loadOrCreate(playerId, jobKey));
  }

  private PlayerUpgradeDataImpl loadOrCreate(String playerId, String jobKey) {
    PlayerUpgradeDataImpl loaded = repository.loadPlayerData(playerId, jobKey);
    if (loaded != null) {
      return loaded;
    }

    // Calculate retroactive skill points based on current job level
    int retroactiveSkillPoints = calculateRetroactiveSkillPoints(playerId, jobKey);

    if (retroactiveSkillPoints > 0) {
      // Create new data with calculated skill points
      PlayerUpgradeDataImpl newData = new PlayerUpgradeDataImpl(playerId, jobKey, retroactiveSkillPoints, Set.of());
      // Save to database immediately
      repository.savePlayerData(newData);
      return newData;
    }

    return PlayerUpgradeDataImpl.empty(playerId, jobKey);
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
      UUID uuid = UUID.fromString(playerId);
      OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

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
