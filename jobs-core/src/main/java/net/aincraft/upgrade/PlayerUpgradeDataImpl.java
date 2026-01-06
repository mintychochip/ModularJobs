package net.aincraft.upgrade;

import java.util.Collections;
import java.util.HashSet;
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

  public PlayerUpgradeDataImpl(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes
  ) {
    this.playerId = playerId;
    this.jobKey = jobKey;
    this.totalSkillPoints = totalSkillPoints;
    this.unlockedNodes = new HashSet<>(unlockedNodes);
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
}
