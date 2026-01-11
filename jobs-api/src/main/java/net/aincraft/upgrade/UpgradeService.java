package net.aincraft.upgrade;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing player upgrades within job upgrade trees.
 */
public interface UpgradeService {

  /**
   * Get the upgrade tree for a job.
   *
   * @param jobKey the job key
   * @return the upgrade tree, or empty if job has no tree
   */
  @NotNull Optional<UpgradeTree> getTree(@NotNull String jobKey);

  /**
   * Get all loaded upgrade trees.
   *
   * @return collection of all upgrade trees
   */
  @NotNull Collection<UpgradeTree> getAllTrees();

  /**
   * Get a player's upgrade data for a specific job.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return the player's upgrade data
   */
  @NotNull PlayerUpgradeData getPlayerData(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Get all available (unlockable) nodes for a player in a job.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return set of nodes that can be unlocked
   */
  @NotNull Set<UpgradeNode> getAvailableNodes(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Attempt to unlock a node for a player.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @param nodeKey  the node to unlock
   * @return result of the unlock attempt
   */
  @NotNull UnlockResult unlock(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Award skill points to a player for a job (typically called on level-up).
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @param points   number of points to award
   */
  void awardSkillPoints(@NotNull String playerId, @NotNull String jobKey, int points);

  /**
   * Reset all upgrades for a player in a job, refunding skill points.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return true if reset was successful
   */
  boolean resetUpgrades(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Attempt to upgrade an already-unlocked node to the next level.
   * Only works for nodes with maxLevel > 1 (upgradeable nodes).
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @param nodeKey  the node to upgrade
   * @return result of the upgrade attempt
   */
  @NotNull UnlockResult upgradeNode(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Result of an unlock or upgrade attempt.
   */
  sealed interface UnlockResult permits
      UnlockResult.Success,
      UnlockResult.InsufficientPoints,
      UnlockResult.PrerequisitesNotMet,
      UnlockResult.OrPrerequisitesNotMet,
      UnlockResult.ExcludedByChoice,
      UnlockResult.AlreadyUnlocked,
      UnlockResult.NodeNotFound,
      UnlockResult.TreeNotFound,
      UnlockResult.NodeUpgraded,
      UnlockResult.AlreadyMaxLevel,
      UnlockResult.NodeNotUnlocked {

    record Success(@NotNull UpgradeNode node, int remainingPoints) implements UnlockResult {
    }

    record InsufficientPoints(int required, int available) implements UnlockResult {
    }

    record PrerequisitesNotMet(@NotNull Set<String> missing) implements UnlockResult {
    }

    /**
     * None of the OR prerequisites were met (at least one required).
     */
    record OrPrerequisitesNotMet(@NotNull Set<String> options) implements UnlockResult {
    }

    record ExcludedByChoice(@NotNull Set<String> conflicting) implements UnlockResult {
    }

    record AlreadyUnlocked(@NotNull String nodeKey) implements UnlockResult {
    }

    record NodeNotFound(@NotNull String nodeKey) implements UnlockResult {
    }

    record TreeNotFound(@NotNull String jobKey) implements UnlockResult {
    }

    /**
     * Node was successfully upgraded to the next level.
     */
    record NodeUpgraded(@NotNull String nodeKey, int newLevel, int maxLevel, int remainingPoints) implements UnlockResult {
    }

    /**
     * Node is already at maximum level.
     */
    record AlreadyMaxLevel(@NotNull String nodeKey, int maxLevel) implements UnlockResult {
    }

    /**
     * Node has not been unlocked yet (cannot upgrade).
     */
    record NodeNotUnlocked(@NotNull String nodeKey) implements UnlockResult {
    }
  }
}
