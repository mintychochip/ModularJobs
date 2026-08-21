package dev.mintychochip.service;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Station tier gate for crafting and gathering.
 */
public interface StationService {

  /**
   * @param stationType logical station id (e.g. {@code forge}, {@code campfire})
   * @param requiredTier minimum tier required by the recipe/action
   * @return true if the player may use a station of this type at the required tier
   */
  boolean canUseStation(
      @NotNull UUID playerId,
      @NotNull String stationType,
      int requiredTier);

  /**
   * Effective tier available for a station type. Stub may return a high constant.
   */
  int getStationTier(@NotNull String stationType);
}
