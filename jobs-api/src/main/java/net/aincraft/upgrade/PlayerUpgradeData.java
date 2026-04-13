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
   * Get current upgrade level for a node (1 = unlocked at base, 2+ = upgraded).
   * Returns 0 if node not unlocked.
   *
   * @param nodeKey the node key
   * @return current level (0 if not unlocked, 1+ if unlocked)
   */
  int getNodeLevel(@NotNull String nodeKey);
}
