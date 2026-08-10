package net.aincraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.aincraft.profession.FishCatchGateStore;
import net.aincraft.test.MockBukkitSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class PluginContextFishCatchGateConfigTest {

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void loadsFishGatesFromPluginConfig() {
    PluginMock plugin = PluginMock.builder()
        .withPluginName("ModularJobs")
        .build();
    plugin.getConfig().set("fish-catch-gates.salmon.profession", "fisherman");
    plugin.getConfig().set("fish-catch-gates.salmon.level", 10);

    FishCatchGateStore store = PluginContext.loadFishCatchGates(plugin);

    assertTrue(store.gateFor("salmon").isPresent());
    assertEquals("fishing", store.gateFor("salmon").orElseThrow().professionId());
    assertEquals(10, store.gateFor("salmon").orElseThrow().minLevel());
  }
}
