package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.registry.Registry;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of UpgradeService.
 */
@Singleton
public final class UpgradeServiceImpl implements UpgradeService {

  private final Registry<UpgradeTree> treeRegistry;
  private final PlayerUpgradeRepository repository;

  // In-memory cache: playerId -> jobKey -> data
  private final Map<String, Map<String, PlayerUpgradeDataImpl>> cache = new ConcurrentHashMap<>();

  @Inject
  public UpgradeServiceImpl(
      Registry<UpgradeTree> treeRegistry,
      PlayerUpgradeRepository repository
  ) {
    this.treeRegistry = treeRegistry;
    this.repository = repository;
  }

  @Override
  public @NotNull Optional<UpgradeTree> getTree(@NotNull String jobKey) {
    return treeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(jobKey))
        .findFirst();
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

    // Clear all unlocks but keep total skill points
    Set<String> unlocked = new HashSet<>(data.unlockedNodes());
    for (String nodeKey : unlocked) {
      data.lock(nodeKey);
    }

    repository.savePlayerData(data);
    return true;
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
    return PlayerUpgradeDataImpl.empty(playerId, jobKey);
  }
}
