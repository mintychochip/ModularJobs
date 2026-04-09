package net.aincraft.upgrade;

import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player's upgrade data for a specific job.
 * Tracks unlocked nodes and available skill points.
 */
public interface PlayerUpgradeData {

  /**
   * The player's UUID as string.
   */
  @NotNull String playerId();

  /**
   * The job key this data belongs to.
   */
  @NotNull String jobKey();

  /**
   * Total skill points earned (from leveling).
   */
  int totalSkillPoints();

  /**
   * Skill points currently available to spend.
   */
  int availableSkillPoints();

  /**
   * Skill points already spent on upgrades.
   */
  int spentSkillPoints();

  /**
   * Set of unlocked node keys.
   */
  @NotNull Set<String> unlockedNodes();

  /**
   * Check if a specific node is unlocked.
   */
  boolean hasUnlocked(@NotNull String nodeKey);

  /**
   * Map of perk levels (perkId -> max level unlocked).
   * Only contains perks that have been unlocked (level >= 1).
   */
  @NotNull Map<String, Integer> perkLevels();

  /**
   * Get the current level of a perk.
   * @return perk level (0 if not unlocked, else the max level unlocked)
   */
  default int getPerkLevel(@NotNull String perkId) {
    return perkLevels().getOrDefault(perkId, 0);
  }

  /**
   * Get the maximum level for a perk in this job's upgrade tree.
   * This is determined by the upgrade tree configuration (max_level on nodes).
   * @param perkId the perk ID to check
   * @return max level achievable for this perk, or 1 if unknown
   */
  int getMaxLevel(@NotNull String perkId);

  /**
   * Check if a perk is at its maximum level.
   * @param perkId the perk ID to check
   * @return true if perk level equals max level, false otherwise
   */
  default boolean isMaxLevel(@NotNull String perkId) {
    int current = getPerkLevel(perkId);
    int max = getMaxLevel(perkId);
    return current > 0 && current >= max;
  }
}
