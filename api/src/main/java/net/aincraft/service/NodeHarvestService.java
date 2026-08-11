package net.aincraft.service;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * World gather hook for resource nodes.
 */
public interface NodeHarvestService {

  /**
   * @param materialTags tags for materials awarded (items plugin / inventory later)
   * @param xpTier       resource tier tag (1–5) for callers / content
   * @param nodeId       stable node identity for cooldowns
   * @param success      whether harvest granted anything
   */
  record HarvestResult(
      boolean success,
      @NotNull List<String> materialTags,
      int xpTier,
      @NotNull String nodeId
  ) {
    public static HarvestResult empty() {
      return new HarvestResult(false, List.of(), 1, "");
    }
  }

  /**
   * Attempt to harvest a resource node at a block location.
   */
  @NotNull
  HarvestResult tryHarvest(
      @NotNull UUID playerId,
      @NotNull String worldName,
      int x,
      int y,
      int z);
}
