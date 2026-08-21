package net.aincraft.boost.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.conditions.SnapshotCondition;
import dev.conditions.SneakingCondition;
import dev.conditions.WorldCondition;
import net.aincraft.boost.config.BoostSourceConfig.BoostConfig;
import net.aincraft.boost.config.BoostSourceConfig.ConditionConfig;
import net.aincraft.boost.config.BoostSourceConfig.RuleConfig;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Config parse must accept world-by-name and default condition/boost types without
 * requiring live Bukkit worlds.
 */
class BoostConfigParseTest {

  private BoostSourceConfigParser parser;
  private ConditionConfigParser conditionParser;

  @BeforeEach
  void setUp() {
    BoostFactoryImpl factory = BoostFactoryImpl.INSTANCE;
    parser = new BoostSourceConfigParser(factory, factory);
    conditionParser = new ConditionConfigParser(factory);
  }

  @Test
  void parseWorldByNameDoesNotRequireWorldToExist() {
    ConditionConfig config = new ConditionConfig(
        "world", null, "world_nether", null, null, null, null, null, null, null, null, null
    );
    Condition condition = assertDoesNotThrow(() -> conditionParser.parse(config));
    WorldCondition world = assertInstanceOf(WorldCondition.class, SnapshotCondition.unwrap(condition));
    assertEquals("world_nether", world.worldName());
  }

  @Test
  void parseWorldByNamespacedKey() {
    ConditionConfig config = new ConditionConfig(
        "world", null, "minecraft:the_end", null, null, null, null, null, null, null, null, null
    );
    Condition condition = conditionParser.parse(config);
    WorldCondition world = assertInstanceOf(WorldCondition.class, SnapshotCondition.unwrap(condition));
    assertEquals("minecraft:the_end", world.worldName());
  }

  @Test
  void parseAlwaysSneakingPlayerResourceAndOr() {
    assertDoesNotThrow(() -> conditionParser.parse(
        new ConditionConfig("always", null, null, null, null, null, null, null, null, null, null, null)));

    Condition sneak = conditionParser.parse(
        new ConditionConfig("sneaking", null, true, null, null, null, null, null, null, null, null, null));
    assertInstanceOf(SneakingCondition.class, SnapshotCondition.unwrap(sneak));

    Condition resource = conditionParser.parse(
        new ConditionConfig(
            "player_resource", "less_than_or_equal", 6.0,
            null, null, null, "health", null, null, null, null, null));
    assertNotNull(resource);

    Condition and = conditionParser.parse(
        new ConditionConfig(
            "and", null, null, null,
            List.of(
                new ConditionConfig("sneaking", null, true, null, null, null, null, null, null, null, null, null),
                new ConditionConfig("world", null, "world_nether", null, null, null, null, null, null, null, null, null)
            ),
            null, null, null, null, null, null, null));
    assertNotNull(and);

    Condition or = conditionParser.parse(
        new ConditionConfig(
            "or", null, null, null,
            List.of(
                new ConditionConfig("sneaking", null, true, null, null, null, null, null, null, null, null, null),
                new ConditionConfig("sprinting", null, false, null, null, null, null, null, null, null, null, null)
            ),
            null, null, null, null, null, null, null));
    assertNotNull(or);
  }

  @Test
  void parseMiningBoostSourceShapeFromDefaultConfig() {
    // Matches boost_sources_default.json mining_boost: high world+sneak, low always
    BoostSourceConfig config = new BoostSourceConfig(
        "modularjobs:mining_boost",
        "Enhanced mining rewards",
        null,
        List.of(
            new RuleConfig(
                100,
                new ConditionConfig(
                    "and", null, null, null,
                    List.of(
                        new ConditionConfig("world", null, "world_nether", null, null, null, null, null, null, null, null, null),
                        new ConditionConfig("sneaking", null, true, null, null, null, null, null, null, null, null, null)
                    ),
                    null, null, null, null, null, null, null
                ),
                new BoostConfig("multiplicative", 3.0)
            ),
            new RuleConfig(
                10,
                new ConditionConfig("always", null, null, null, null, null, null, null, null, null, null, null),
                new BoostConfig("multiplicative", 1.25)
            )
        )
    );

    BoostSource source = assertDoesNotThrow(() -> parser.parse(config));
    RuledBoostSource ruled = assertInstanceOf(RuledBoostSourceImpl.class, source);
    assertEquals(Key.key("modularjobs", "mining_boost"), ruled.key());
    assertEquals(2, ruled.rules().size());
    assertEquals(100, ruled.rules().get(0).priority());
    assertEquals(10, ruled.rules().get(1).priority());
    assertInstanceOf(MultiplicativeBoostImpl.class, ruled.rules().get(0).boost());
  }

  @Test
  void loadDefaultJsonResourceParsesWithoutLiveWorlds() throws Exception {
    try (InputStream in = getClass().getClassLoader()
        .getResourceAsStream("boost_sources_default.json")) {
      assertNotNull(in, "boost_sources_default.json must be on test classpath");
      Gson gson = new Gson();
      JsonObject root = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
      assertTrue(root.has("boost_sources"));
      JsonObject sources = root.getAsJsonObject("boost_sources");

      int parsed = 0;
      for (String name : sources.keySet()) {
        BoostSourceConfig config = gson.fromJson(sources.get(name), BoostSourceConfig.class);
        BoostSource source = assertDoesNotThrow(
            () -> parser.parse(config),
            "failed to parse boost source: " + name
        );
        assertNotNull(source);
        parsed++;
      }
      assertTrue(parsed >= 5, "expected several default sources, got " + parsed);
    }
  }

  @Test
  void additiveAndMultiplicativeBoostParse() {
    Boost multi = parser.parseBoost(new BoostConfig("multiplicative", 2.5));
    Boost add = parser.parseBoost(new BoostConfig("additive", 10));
    assertEquals(0, new BigDecimal("250.0").compareTo(multi.boost(new BigDecimal("100"))));
    assertEquals(0, new BigDecimal("110").compareTo(add.boost(new BigDecimal("100"))));
  }
}
