package net.aincraft.upgrade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository for persisting player upgrade data.
 */
public interface PlayerUpgradeRepository {

  /**
   * Load player upgrade data for a specific job.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return the player's upgrade data, or null if none exists
   */
  @Nullable PlayerUpgradeDataImpl loadPlayerData(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Save player upgrade data.
   *
   * @param data the data to save
   */
  void savePlayerData(@NotNull PlayerUpgradeDataImpl data);

  /**
   * Delete player upgrade data for a job.
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return true if data was deleted
   */
  boolean deletePlayerData(@NotNull String playerId, @NotNull String jobKey);
}
