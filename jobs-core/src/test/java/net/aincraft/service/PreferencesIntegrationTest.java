package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.aincraft.test.MockBukkitSupport;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Soft-integration path: when Preferences is not installed, {@link PreferencesIntegration}
 * must still enable-safe-fall back to local defaults (no hard crash).
 */
class PreferencesIntegrationTest {

  private JavaPlugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
    plugin.getConfig().set("preferences.entries-per-page", 8);
    plugin.getConfig().set("preferences.default-gui-mode", true);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void wireWithoutPreferencesPluginUsesLocalImplAndNoUnregisterHook() {
    // MockBukkit has no Preferences plugin → soft fallback
    PreferencesIntegration.Wiring wiring = PreferencesIntegration.wire(plugin);

    assertNotNull(wiring.service());
    assertInstanceOf(PreferencesServiceImpl.class, wiring.service());
    assertNull(wiring.onDisable(), "no external unregister when Preferences is absent");

    PlayerMock player = MockBukkitSupport.mockServer().addPlayer("integration");
    assertEqualsDefaultFromConfig(wiring.service(), player);
  }

  private static void assertEqualsDefaultFromConfig(
      PreferencesService service, PlayerMock player) {
    assertTrue(service.getEntriesPerPage(player) == 8);
    assertTrue(service.prefersGuiMode(player));
  }
}
