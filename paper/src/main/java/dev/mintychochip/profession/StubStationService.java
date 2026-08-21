package dev.mintychochip.profession;

import dev.mintychochip.service.StationService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/** In-memory station tier defaults for configured station types. */
public final class StubStationService implements StationService {

  public static final int DEFAULT_TIER = 5;

  private final Map<String, Integer> tiers = new ConcurrentHashMap<>();
  private final int defaultTier;

  /** Stub station service. */
  public StubStationService() {
    this(DEFAULT_TIER);
  }

  /** Stub station service. */
  public StubStationService(int defaultTier) {
    this.defaultTier = defaultTier;
  }

  /** Test/admin override for a station type. */
  public void setStationTier(@NotNull String stationType, int tier) {
    tiers.put(stationType.toLowerCase(), tier);
  }

  @Override
  public boolean canUseStation(
      @NotNull UUID playerId, @NotNull String stationType, int requiredTier) {
    return getStationTier(stationType) >= requiredTier;
  }

  @Override
  public int getStationTier(@NotNull String stationType) {
    return tiers.getOrDefault(stationType.toLowerCase(), defaultTier);
  }
}
