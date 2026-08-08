package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlBlockBreakGateLoaderTest {

  private YamlBlockBreakGateLoader loader;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    loader = new YamlBlockBreakGateLoader(Logger.getLogger("test"));
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void parsesValidGateEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("block-break-gates.diamond_ore.profession", "mining");
    config.set("block-break-gates.diamond_ore.level", 30);
    config.set("block-break-gates.ancient_debris.profession", "mining");
    config.set("block-break-gates.ancient_debris.level", 40);

    List<BlockBreakGate> gates = loader.load(config);

    assertEquals(2, gates.size());
    BlockBreakGate diamond = gates.stream()
        .filter(g -> g.materialKey().equals("diamond_ore"))
        .findFirst().orElseThrow();
    assertEquals("mining", diamond.professionId());
    assertEquals(30, diamond.minLevel());
    assertTrue(Material.matchMaterial(diamond.materialKey()) != null);
  }

  @Test
  void skipsInvalidEntriesWithWarnings() {
    YamlConfiguration config = new YamlConfiguration();
    // Unknown material
    config.set("block-break-gates.not_a_real_material.profession", "mining");
    config.set("block-break-gates.not_a_real_material.level", 30);
    // Unknown profession
    config.set("block-break-gates.stone.profession", "not_a_profession");
    config.set("block-break-gates.stone.level", 5);
    // Non-int level
    config.set("block-break-gates.dirt.profession", "farming");
    config.set("block-break-gates.dirt.level", "high");
    // Zero/negative level
    config.set("block-break-gates.sand.profession", "mining");
    config.set("block-break-gates.sand.level", 0);

    List<BlockBreakGate> gates = loader.load(config);
    assertTrue(gates.isEmpty());
  }

  @Test
  void emptyOrMissingSectionYieldsEmptyList() {
    YamlConfiguration empty = new YamlConfiguration();
    assertTrue(loader.load(empty).isEmpty());
  }

  @Test
  void storeLooksUpCaseInsensitively() {
    BlockBreakGateStore store = new BlockBreakGateStore(
        List.of(new BlockBreakGate("diamond_ore", "mining", 30)));
    assertTrue(store.gateFor("DIAMOND_ORE").isPresent());
    assertTrue(store.gateFor("stone").isEmpty());
    assertFalse(store.isEmpty());
    assertTrue(new BlockBreakGateStore(List.of()).isEmpty());
  }
}
