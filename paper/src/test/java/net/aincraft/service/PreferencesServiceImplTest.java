package net.aincraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.aincraft.test.MockBukkitSupport;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Local fallback path when the Preferences plugin / service is absent.
 * Exercises shipped {@link PreferencesServiceImpl} get/set and config defaults.
 */
class PreferencesServiceImplTest {

  private JavaPlugin plugin;
  private PlayerMock player;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
    plugin.getConfig().set("preferences.entries-per-page", 12);
    plugin.getConfig().set("preferences.default-gui-mode", false);
    player = MockBukkitSupport.mockServer().addPlayer("local-pref");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void absentServiceFallsBackToConfigDefaultsWithoutThrowing() {
    PreferencesService service = new PreferencesServiceImpl(plugin);

    assertEquals(12, service.getDefaultEntriesPerPage());
    assertEquals(12, service.getEntriesPerPage(player));
    assertFalse(service.prefersGuiMode(player));
  }

  @Test
  void localSetThenGetRoundTrip() {
    PreferencesService service = new PreferencesServiceImpl(plugin);

    service.setEntriesPerPage(player, 20);
    service.setGuiMode(player, true);

    assertEquals(20, service.getEntriesPerPage(player));
    assertTrue(service.prefersGuiMode(player));
  }

  @Test
  void localSetClampsEntriesPerPage() {
    PreferencesService service = new PreferencesServiceImpl(plugin);

    service.setEntriesPerPage(player, -5);
    assertEquals(1, service.getEntriesPerPage(player));

    service.setEntriesPerPage(player, 100);
    assertEquals(50, service.getEntriesPerPage(player));
  }
}
