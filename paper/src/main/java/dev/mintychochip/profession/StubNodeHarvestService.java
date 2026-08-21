package dev.mintychochip.profession;

import dev.mintychochip.service.NodeHarvestService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory gather stub keyed by resource location.
 *
 * <p>Register nodes via {@link #registerNode}; unregistered locations return empty.
 */
public final class StubNodeHarvestService implements NodeHarvestService {

  private final Map<String, HarvestResult> nodes = new ConcurrentHashMap<>();

  /** Register node. */
  public void registerNode(
      @NotNull String worldName,
      int x,
      int y,
      int z,
      @NotNull String nodeId,
      int xpTier,
      @NotNull List<String> materialTags) {
    nodes.put(
        locationKey(worldName, x, y, z),
        new HarvestResult(true, List.copyOf(materialTags), xpTier, nodeId));
  }

  /** Clear nodes. */
  public void clearNodes() {
    nodes.clear();
  }

  @Override
  public @NotNull HarvestResult tryHarvest(
      @NotNull UUID playerId, @NotNull String worldName, int x, int y, int z) {
    HarvestResult result = nodes.get(locationKey(worldName, x, y, z));
    return result != null ? result : HarvestResult.empty();
  }

  private static String locationKey(String world, int x, int y, int z) {
    return world.toLowerCase(Locale.ROOT) + "|" + x + "|" + y + "|" + z;
  }
}
