package net.aincraft.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * A player's upgrade data for a specific job: unlocked nodes and skill points.
 * Mutable for service use; callers that only read should treat it as opaque state.
 */
public final class PlayerUpgradeData {

  private final String playerId;
  private final String jobKey;
  private int totalSkillPoints;
  private final Set<String> unlockedNodes;
  private final Map<String, Integer> perkLevels;
  private final Map<String, Integer> maxLevels; // perkId -> max achievable level

  public PlayerUpgradeData(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes
  ) {
    this.playerId = playerId;
    this.jobKey = jobKey;
    this.totalSkillPoints = totalSkillPoints;
    this.unlockedNodes = new HashSet<>(unlockedNodes);
    this.perkLevels = new HashMap<>();
    this.maxLevels = new HashMap<>();
  }

  /**
   * Create empty upgrade data for a new player-job combination.
   */
  public static PlayerUpgradeData empty(@NotNull String playerId, @NotNull String jobKey) {
    return new PlayerUpgradeData(playerId, jobKey, 0, Set.of());
  }

  public @NotNull String playerId() {
    return playerId;
  }

  public @NotNull String jobKey() {
    return jobKey;
  }

  public int totalSkillPoints() {
    return totalSkillPoints;
  }

  public int availableSkillPoints() {
    return totalSkillPoints - spentSkillPoints();
  }

  public int spentSkillPoints() {
    // Placeholder - should sum actual node costs
    return unlockedNodes.size();
  }

  public @NotNull Set<String> unlockedNodes() {
    return Collections.unmodifiableSet(unlockedNodes);
  }

  public @NotNull Map<String, Integer> perkLevels() {
    return Collections.unmodifiableMap(perkLevels);
  }

  public boolean hasUnlocked(@NotNull String nodeKey) {
    return unlockedNodes.contains(nodeKey);
  }

  /**
   * Get the current level of a perk.
   * @return perk level (0 if not unlocked, else the max level unlocked)
   */
  public int getPerkLevel(@NotNull String perkId) {
    return perkLevels.getOrDefault(perkId, 0);
  }

  /**
   * Get the maximum level for a perk in this job's upgrade tree.
   * @return max level achievable for this perk, or 1 if unknown
   */
  public int getMaxLevel(@NotNull String perkId) {
    return maxLevels.getOrDefault(perkId, 1);
  }

  /**
   * Check if a perk is at its maximum level.
   */
  public boolean isMaxLevel(@NotNull String perkId) {
    int current = getPerkLevel(perkId);
    int max = getMaxLevel(perkId);
    return current > 0 && current >= max;
  }

  // Mutators for service use

  public void addSkillPoints(int points) {
    this.totalSkillPoints += points;
  }

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

  /**
   * Set the max level for a perk (from upgrade tree config).
   */
  public void setMaxLevel(@NotNull String perkId, int maxLevel) {
    maxLevels.put(perkId, maxLevel);
  }
}
