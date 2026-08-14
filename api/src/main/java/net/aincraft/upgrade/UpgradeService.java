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
   * Get the version-2 skill tree for a job, when one is loaded.
   *
   * @param jobKey the job key
   * @return the skill tree, or empty if the job has no v2 tree
   */
  @NotNull Optional<SkillTree> getSkillTree(@NotNull String jobKey);

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
   * Get a player's current skill tree state for a job.
   */
  @NotNull SkillTreeState getSkillTreeState(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Purchase the next level of a skill node.
   */
  @NotNull PurchaseResult purchaseSkillLevel(
      @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Purchase (permanently choose) a major node.
   */
  @NotNull PurchaseResult purchaseMajor(
      @NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Reset all skill levels for a player in a job.
   *
   * @return true if reset was successful
   */
  boolean resetTree(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Result of a skill-node purchase attempt.
   */
  sealed interface PurchaseResult permits
      PurchaseResult.Success,
      PurchaseResult.InsufficientPoints,
      PurchaseResult.RequirementsNotMet,
      PurchaseResult.PrerequisitesNotMet,
      PurchaseResult.ExcludedByChoice,
      PurchaseResult.AlreadyOwned,
      PurchaseResult.NodeNotFound,
      PurchaseResult.TreeNotFound {

    record Success(@NotNull SkillNode node, int remainingPoints) implements PurchaseResult {
    }

    record InsufficientPoints(int required, int available) implements PurchaseResult {
    }

    record RequirementsNotMet(@NotNull Set<String> unmet) implements PurchaseResult {
    }

    record PrerequisitesNotMet(@NotNull Set<String> missing) implements PurchaseResult {
    }

    record ExcludedByChoice(@NotNull Set<String> conflicting) implements PurchaseResult {
    }

    record AlreadyOwned(@NotNull String nodeKey) implements PurchaseResult {
    }

    record NodeNotFound(@NotNull String nodeKey) implements PurchaseResult {
    }

    record TreeNotFound(@NotNull String jobKey) implements PurchaseResult {
    }
  }

  /**
   * Result of an unlock attempt.
   */
  sealed interface UnlockResult permits
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

  /**
   * Remove persisted state for a player leaving a job and revoke its effects.
   */
  void clearTreeState(@NotNull String playerId, @NotNull String jobKey);
}
