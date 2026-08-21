package net.aincraft.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.cache.CacheLoader;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;
import net.aincraft.test.MockBukkitSupport;
import net.aincraft.util.LocationKey;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves placed-block exploit materials are not STONE-only: defaults and config-driven maps
 * cover additional break-farm materials, and the shipped store honors them.
 */
class PlacedProtectionMaterialsTest {

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void defaultsProtectMoreThanStone() {
    Map<Material, Duration> map = PlacedProtectionMaterials.defaults();
    assertTrue(map.containsKey(Material.STONE));
    assertTrue(map.containsKey(Material.DIRT), "must protect dirt place-break farms");
    assertTrue(map.containsKey(Material.COBBLESTONE));
    assertTrue(map.containsKey(Material.DIAMOND_ORE)
            || map.containsKey(Material.DEEPSLATE_DIAMOND_ORE),
        "must protect ore place-break farms");
    assertTrue(map.size() > 1, "must not be STONE-only (size=" + map.size() + ")");
  }

  @Test
  void starMaterialsLoadsAllBlocks() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("placed.duration-seconds", 30);
    config.set("placed.materials", java.util.List.of("*"));
    Map<Material, Duration> map =
        PlacedProtectionMaterials.fromConfiguration(config, Logger.getGlobal());
    assertTrue(map.size() > 50);
    assertEquals(Duration.ofSeconds(30), map.get(Material.DIRT));
  }

  @Test
  void explicitMaterialListHonored() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("placed.duration-seconds", 10);
    config.set("placed.materials", java.util.List.of("DIRT", "SAND", "GRAVEL"));
    Map<Material, Duration> map =
        PlacedProtectionMaterials.fromConfiguration(config, Logger.getGlobal());
    assertTrue(map.containsKey(Material.DIRT));
    assertTrue(map.containsKey(Material.SAND));
    assertTrue(map.containsKey(Material.GRAVEL));
    assertFalse(map.containsKey(Material.STONE), "STONE not listed");
  }

  @Test
  void exploitServiceCanProtectNonStoneWithDefaults() {
    Map<Material, Duration> placed = PlacedProtectionMaterials.defaults();
    ExploitService service = PaymentWiring.createExploitService(placed);
    assertNotNull(service);

    Map<Material, java.time.temporal.TemporalAmount> temporal = new EnumMap<>(placed);
    MemoryExploitProtectionStoreImpl<Material, Material> store =
        new MemoryExploitProtectionStoreImpl<>(
            temporal,
            m -> m,
            CacheLoader.from(m -> new LocationKey("world", 0, 0, 0)));

    assertTrue(store.canProtect(Material.DIRT));
    assertTrue(store.canProtect(Material.STONE));
    assertTrue(store.canProtect(Material.OAK_LOG));
  }
}
