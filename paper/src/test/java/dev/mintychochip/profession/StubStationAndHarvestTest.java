package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import dev.mintychochip.service.NodeHarvestService.HarvestResult;
import org.junit.jupiter.api.Test;

/**
 * Drives P6 stubs: {@link StubStationService}, {@link StubNodeHarvestService}.
 */
class StubStationAndHarvestTest {

  private static final UUID PLAYER = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");

  @Test
  void stationStubAllowsByDefaultTier() {
    StubStationService stations = new StubStationService();
    assertEquals(StubStationService.DEFAULT_TIER, stations.getStationTier("forge"));
    assertTrue(stations.canUseStation(PLAYER, "forge", 3));
    assertTrue(stations.canUseStation(PLAYER, "forge", 5));
  }

  @Test
  void stationStubRejectsWhenTierTooLow() {
    StubStationService stations = new StubStationService();
    stations.setStationTier("campfire", 1);
    assertFalse(stations.canUseStation(PLAYER, "campfire", 2));
    assertTrue(stations.canUseStation(PLAYER, "campfire", 1));
  }

  @Test
  void harvestEmptyWithoutRegistration() {
    StubNodeHarvestService harvest = new StubNodeHarvestService();
    HarvestResult r = harvest.tryHarvest(PLAYER, "world", 0, 64, 0);
    assertFalse(r.success());
    assertTrue(r.materialTags().isEmpty());
  }

  @Test
  void harvestReturnsRegisteredNode() {
    StubNodeHarvestService harvest = new StubNodeHarvestService();
    harvest.registerNode("world", 10, 70, -5, "iron_node_1", 2, List.of("iron_ore"));
    HarvestResult r = harvest.tryHarvest(PLAYER, "world", 10, 70, -5);
    assertTrue(r.success());
    assertEquals(2, r.xpTier());
    assertEquals("iron_node_1", r.nodeId());
    assertEquals(List.of("iron_ore"), r.materialTags());
  }

  @Test
  void harvestWorldNameCaseInsensitive() {
    StubNodeHarvestService harvest = new StubNodeHarvestService();
    harvest.registerNode("World", 1, 2, 3, "n", 1, List.of("mat"));
    assertTrue(harvest.tryHarvest(PLAYER, "world", 1, 2, 3).success());
  }
}
