package net.aincraft.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of PlayerUpgradeData.
 * Mutable for use in services; immutable view exposed via interface.
 */
public final class PlayerUpgradeDataImpl implements PlayerUpgradeData {

  private final String playerId;
  private final String jobKey;
  private int totalSkillPoints;
  private final Set<String> unlockedNodes;
  private final Map<String, Integer> perkLevels;
  private final Map<String, Integer> nodeLevels;

  public PlayerUpgradeDataImpl(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes
  ) {
    this(playerId, jobKey, totalSkillPoints, unlockedNodes, Map.of());
  }

  public PlayerUpgradeDataImpl(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes,
      @NotNull Map<String, Integer> nodeLevels
  ) {
    this.playerId = playerId;
    this.jobKey = jobKey;
    this.totalSkillPoints = totalSkillPoints;
    this.unlockedNodes = new HashSet<>(unlockedNodes);
    this.perkLevels = new HashMap<>();
    this.nodeLevels = new HashMap<>(nodeLevels);
  }

  /**
   * Create empty upgrade data for a new player-job combination.
   */
  public static PlayerUpgradeDataImpl empty(@NotNull String playerId, @NotNull String jobKey) {
    return new PlayerUpgradeDataImpl(playerId, jobKey, 0, Set.of());
  }

  @Override
  public @NotNull String playerId() {
    return playerId;
  }

  @Override
  public @NotNull String jobKey() {
    return jobKey;
  }

  @Override
  public int totalSkillPoints() {
    return totalSkillPoints;
  }

  @Override
  public int availableSkillPoints() {
    return totalSkillPoints - spentSkillPoints();
  }

  @Override
  public int spentSkillPoints() {
    // This would need to be calculated from actual node costs
    // For now, track it separately or recalculate from tree
    return unlockedNodes.size(); // Placeholder - should sum actual costs
  }

  @Override
  public @NotNull Set<String> unlockedNodes() {
    return Collections.unmodifiableSet(unlockedNodes);
  }

  @Override
  public @NotNull Map<String, Integer> perkLevels() {
    return Collections.unmodifiableMap(perkLevels);
  }

  @Override
  public boolean hasUnlocked(@NotNull String nodeKey) {
    return unlockedNodes.contains(nodeKey);
  }

  // Mutators for service use

  /**
   * Add skill points (e.g., on level up).
   */
  public void addSkillPoints(int points) {
    this.totalSkillPoints += points;
  }

  /**
   * Set total skill points directly.
   */
  public void setTotalSkillPoints(int points) {
    this.totalSkillPoints = points;
  }

  /**
   * Unlock a node.
   *
   * @return true if newly unlocked, false if already unlocked
   */
  public boolean unlock(@NotNull String nodeKey) {
    return unlockedNodes.add(nodeKey);
  }

  /**
   * Remove an unlocked node (for respec functionality).
   *
   * @return true if was unlocked, false if wasn't
   */
  public boolean lock(@NotNull String nodeKey) {
    return unlockedNodes.remove(nodeKey);
  }

  /**
   * Set the level of a perk. Stores the max level.
   *
   * @param perkId perk identifier
   * @param level level to set
   */
  public void setPerkLevel(@NotNull String perkId, int level) {
    int current = perkLevels.getOrDefault(perkId, 0);
    if (level > current) {
      perkLevels.put(perkId, level);
    }
  }

  /**
   * Remove a perk level entry (for respec).
   *
   * @return the previous level, or 0 if not set
   */
  public int removePerkLevel(@NotNull String perkId) {
    Integer previous = perkLevels.remove(perkId);
    return previous != null ? previous : 0;
  }

  @Override
  public int getNodeLevel(@NotNull String nodeKey) {
    return nodeLevels.getOrDefault(nodeKey, hasUnlocked(nodeKey) ? 1 : 0);
  }

  /**
   * Set the level of a node (for in-place upgrades).
   *
   * @param nodeKey node key
   * @param level   level to set (1 = initial unlock, 2+ = upgraded)
   */
  public void setNodeLevel(@NotNull String nodeKey, int level) {
    nodeLevels.put(nodeKey, level);
  }

  /**
   * Remove a node level entry (for respec).
   *
   * @return the previous level, or 0 if not set
   */
  public int removeNodeLevel(@NotNull String nodeKey) {
    Integer previous = nodeLevels.remove(nodeKey);
    return previous != null ? previous : 0;
  }

  /**
   * Get the raw node levels map for repository persistence.
   */
  public @NotNull Map<String, Integer> nodeLevels() {
    return Collections.unmodifiableMap(nodeLevels);
  }
}
