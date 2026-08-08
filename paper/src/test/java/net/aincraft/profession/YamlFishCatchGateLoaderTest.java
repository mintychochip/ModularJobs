package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.aincraft.test.MockBukkitSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlFishCatchGateLoaderTest {

  private YamlFishCatchGateLoader loader;
  private Logger logger;
  private Handler warningHandler;
  private List<String> warnings;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    logger = Logger.getLogger("fish-gate-loader-test");
    logger.setUseParentHandlers(false);
    warnings = new ArrayList<>();
    warningHandler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
          warnings.add(record.getMessage());
        }
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    logger.addHandler(warningHandler);
    loader = new YamlFishCatchGateLoader(logger);
  }

  @AfterEach
  void tearDown() {
    logger.removeHandler(warningHandler);
    warningHandler.close();
    MockBukkitSupport.unmockServer();
  }

  @Test
  void parsesFishEntriesAndCanonicalizesProfessionAlias() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.cod.profession", "fisherman");
    config.set("fish-catch-gates.cod.level", 1);
    config.set("fish-catch-gates.tropical_fish.profession", "fishing");
    config.set("fish-catch-gates.tropical_fish.level", 20);

    List<FishCatchGate> gates = loader.load(config);

    assertEquals(2, gates.size());
    FishCatchGate cod = gates.stream()
        .filter(gate -> gate.itemKey().equals("cod"))
        .findFirst().orElseThrow();
    assertEquals("fishing", cod.professionId());
    assertEquals(1, cod.minLevel());
  }

  @Test
  void skipsUnknownNonFishProfessionAndInvalidLevelEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.not_a_real_material.profession", "fishing");
    config.set("fish-catch-gates.not_a_real_material.level", 1);
    config.set("fish-catch-gates.stone.profession", "fishing");
    config.set("fish-catch-gates.stone.level", 1);
    config.set("fish-catch-gates.cod.profession", "not_a_profession");
    config.set("fish-catch-gates.cod.level", 1);
    config.set("fish-catch-gates.salmon.profession", "fishing");
    config.set("fish-catch-gates.salmon.level", "high");
    config.set("fish-catch-gates.pufferfish.profession", "fishing");
    config.set("fish-catch-gates.pufferfish.level", 0);

    assertTrue(loader.load(config).isEmpty());
  }

  @Test
  void missingSectionYieldsEmptyList() {
    assertTrue(loader.load(new YamlConfiguration()).isEmpty());
  }

  @Test
  void warnsAndSkipsNonSectionEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.cod", true);
    config.set("fish-catch-gates.salmon.profession", "fishing");
    config.set("fish-catch-gates.salmon.level", 10);

    List<FishCatchGate> gates = loader.load(config);

    assertEquals(1, gates.size());
    assertTrue(warnings.stream()
        .anyMatch(message -> message.contains("'cod' must be a configuration section")));
  }

  @Test
  void warnsAndSkipsCaseInsensitiveDuplicateEntries() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("fish-catch-gates.cod.profession", "fishing");
    config.set("fish-catch-gates.cod.level", 1);
    config.set("fish-catch-gates.COD.profession", "fishing");
    config.set("fish-catch-gates.COD.level", 2);

    List<FishCatchGate> gates = loader.load(config);

    assertEquals(1, gates.size());
    assertEquals(1, gates.getFirst().minLevel());
    assertTrue(warnings.stream()
        .anyMatch(message -> message.contains("duplicate item key")
            && message.contains("cod")));
  }

  @Test
  void storeLooksUpCaseInsensitively() {
    FishCatchGateStore store = new FishCatchGateStore(
        List.of(new FishCatchGate("cod", "fishing", 1)));

    assertTrue(store.gateFor("COD").isPresent());
    assertTrue(store.gateFor("salmon").isEmpty());
    assertFalse(store.isEmpty());
    assertTrue(new FishCatchGateStore(List.of()).isEmpty());
  }

  @Test
  void storeKeepsFirstCaseInsensitiveDuplicate() {
    FishCatchGateStore store = new FishCatchGateStore(List.of(
        new FishCatchGate("cod", "fishing", 1),
        new FishCatchGate("COD", "fishing", 2)));

    assertEquals(1, store.gateFor("cod").orElseThrow().minLevel());
  }
}
