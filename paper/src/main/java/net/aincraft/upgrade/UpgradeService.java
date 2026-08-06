package net.aincraft.upgrade;

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
 * Manages player upgrades within job upgrade trees.
 */
public final class UpgradeService {

  private final Registry<UpgradeTree> treeRegistry;
  private final PlayerUpgradeRepository repository;
  private final JobService jobService;
  private final UpgradeEffectApplier effectApplier;

  // In-memory cache: playerId -> jobKey -> data
  private final Map<String, Map<String, PlayerUpgradeData>> cache = new ConcurrentHashMap<>();

  public UpgradeService(
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

  public @NotNull Collection<UpgradeTree> getAllTrees() {
    return treeRegistry.stream().toList();
  }

  public @NotNull PlayerUpgradeData getPlayerData(@NotNull String playerId, @NotNull String jobKey) {
    return getOrLoadData(playerId, jobKey);
  }

  public @NotNull Set<UpgradeNode> getAvailableNodes(@NotNull String playerId, @NotNull String jobKey) {
    Optional<UpgradeTree> treeOpt = getTree(jobKey);
    if (treeOpt.isEmpty()) {
      return Set.of();
    }

    UpgradeTree tree = treeOpt.get();
    PlayerUpgradeData data = getPlayerData(playerId, jobKey);
    return tree.getAvailableNodes(data.unlockedNodes());
  }

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
    PlayerUpgradeData data = getOrLoadData(playerId, jobKey);

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

  public void awardSkillPoints(@NotNull String playerId, @NotNull String jobKey, int points) {
    PlayerUpgradeData data = getOrLoadData(playerId, jobKey);
    data.addSkillPoints(points);
    repository.savePlayerData(data);
  }

  public boolean resetUpgrades(@NotNull String playerId, @NotNull String jobKey) {
    PlayerUpgradeData data = getOrLoadData(playerId, jobKey);

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

  private PlayerUpgradeData getOrLoadData(String playerId, String jobKey) {
    return cache
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .computeIfAbsent(jobKey, k -> loadOrCreate(playerId, jobKey));
  }

  private PlayerUpgradeData loadOrCreate(String playerId, String jobKey) {
    PlayerUpgradeData loaded = repository.loadPlayerData(playerId, jobKey);
    if (loaded != null) {
      return loaded;
    }

    // Calculate retroactive skill points based on current job level
    int retroactiveSkillPoints = calculateRetroactiveSkillPoints(playerId, jobKey);

    if (retroactiveSkillPoints > 0) {
      // Create new data with calculated skill points
      PlayerUpgradeData newData = new PlayerUpgradeData(playerId, jobKey, retroactiveSkillPoints, Set.of());
      // Save to database immediately
      repository.savePlayerData(newData);
      return newData;
    }

    return PlayerUpgradeData.empty(playerId, jobKey);
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

  /**
   * Result of an unlock attempt.
   */
  public sealed interface UnlockResult permits
      UnlockResult.Success,
      UnlockResult.InsufficientPoints,
      UnlockResult.PrerequisitesNotMet,
      UnlockResult.ExcludedByChoice,
      UnlockResult.AlreadyUnlocked,
      UnlockResult.NodeNotFound,
      UnlockResult.TreeNotFound {

    record Success(@NotNull UpgradeNode node, int remainingPoints) implements UnlockResult {
    }

    record InsufficientPoints(int required, int available) implements UnlockResult {
    }

    record PrerequisitesNotMet(@NotNull Set<String> missing) implements UnlockResult {
    }

    record ExcludedByChoice(@NotNull Set<String> conflicting) implements UnlockResult {
    }

    record AlreadyUnlocked(@NotNull String nodeKey) implements UnlockResult {
    }

    record NodeNotFound(@NotNull String nodeKey) implements UnlockResult {
    }

    record TreeNotFound(@NotNull String jobKey) implements UnlockResult {
    }
  }
}
