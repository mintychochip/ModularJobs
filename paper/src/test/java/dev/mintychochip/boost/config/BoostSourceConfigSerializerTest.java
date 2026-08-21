package dev.mintychochip.boost.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.boost.RuledBoostSourceImpl;
import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.boost.config.BoostSourceConfig.BoostConfig;
import dev.mintychochip.boost.config.BoostSourceConfig.ConditionConfig;
import dev.mintychochip.boost.config.BoostSourceConfig.RuleConfig;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import java.math.BigDecimal;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoostSourceConfigSerializerTest {

  private BoostSourceConfigParser parser;

  @BeforeEach
  void setUp() {
    BoostFactoryImpl factory = BoostFactoryImpl.INSTANCE;
    parser = new BoostSourceConfigParser(factory, factory);
  }

  @Test
  void serializeAlwaysAndMultiplicative() {
    Rule rule =
        new Rule(
            SnapshotCondition.wrap(dev.mintychochip.databag.Conditions.always()),
            10,
            new MultiplicativeBoostImpl(BigDecimal.valueOf(1.25)));
    BoostSource source =
        new RuledBoostSourceImpl(List.of(rule), Key.key("modularjobs", "test"), "test desc");

    BoostSourceConfig config = BoostSourceConfigSerializer.serialize(source);
    assertEquals("modularjobs:test", config.key());
    assertEquals("test desc", config.description());
    assertEquals(1, config.rules().size());

    RuleConfig ruleConfig = config.rules().getFirst();
    assertEquals(10, ruleConfig.priority());
    assertEquals("always", ruleConfig.conditions().type());
    assertEquals("multiplicative", ruleConfig.boost().type());
    assertEquals(1.25, ruleConfig.boost().amount(), 1e-9);
  }

  @Test
  void serializeCompositeAndWorldSneakingRoundTrip() {
    Condition world = BoostFactoryImpl.INSTANCE.world("world_nether");
    Condition sneak = BoostFactoryImpl.INSTANCE.sneaking(true);
    Condition and = BoostFactoryImpl.INSTANCE.compose(world, sneak, LogicalOperator.AND);
    Rule rule = new Rule(and, 100, new MultiplicativeBoostImpl(BigDecimal.valueOf(3.0)));
    BoostSource source =
        new RuledBoostSourceImpl(
            List.of(rule), Key.key("modularjobs", "mining_boost"), "nether sneak");

    BoostSourceConfig serialized = BoostSourceConfigSerializer.serialize(source);
    BoostSource reparsed = parser.parse(serialized);

    assertInstanceOf(RuledBoostSourceImpl.class, reparsed);
    RuledBoostSourceImpl ruled = (RuledBoostSourceImpl) reparsed;
    assertEquals(1, ruled.rules().size());
    Rule reparsedRule = ruled.rules().getFirst();
    assertEquals(100, reparsedRule.priority());
    assertInstanceOf(SnapshotCondition.class, reparsedRule.condition());
  }

  @Test
  void serializeWorldPrefersPlainName() {
    Condition world = BoostFactoryImpl.INSTANCE.world("world_nether");
    ConditionConfig config = BoostSourceConfigSerializer.serializeCondition(world);
    assertEquals("world", config.type());
    assertEquals("world_nether", config.value());
  }

  @Test
  void serializeSneaking() {
    ConditionConfig config =
        BoostSourceConfigSerializer.serializeCondition(BoostFactoryImpl.INSTANCE.sneaking(true));
    assertEquals("sneaking", config.type());
    assertEquals(true, config.value());
  }

  @Test
  void serializeFlattenedAnd() {
    Condition a = BoostFactoryImpl.INSTANCE.sneaking(true);
    Condition b = BoostFactoryImpl.INSTANCE.sprinting(false);
    Condition c =
        BoostFactoryImpl.INSTANCE.weather(dev.mintychochip.container.boost.WeatherState.RAINING);
    Condition composite =
        BoostFactoryImpl.INSTANCE.compose(
            BoostFactoryImpl.INSTANCE.compose(a, b, LogicalOperator.AND), c, LogicalOperator.AND);

    ConditionConfig config = BoostSourceConfigSerializer.serializeCondition(composite);
    assertEquals("and", config.type());
    assertNotNull(config.conditions());
    assertEquals(3, config.conditions().size());
  }

  @Test
  void defaultMiningBoostRoundTripFromParserShape() {
    ConditionConfig nether =
        new ConditionConfig(
            "world", null, "world_nether", null, null, null, null, null, null, null, null, null);
    ConditionConfig sneak =
        new ConditionConfig(
            "sneaking", null, true, null, null, null, null, null, null, null, null, null);
    ConditionConfig and =
        new ConditionConfig(
            "and",
            null,
            null,
            null,
            List.of(nether, sneak),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    BoostConfig mult = new BoostConfig("multiplicative", 3.0);
    RuleConfig high = new RuleConfig(100, and, mult);

    ConditionConfig always =
        new ConditionConfig(
            "always", null, null, null, null, null, null, null, null, null, null, null);
    RuleConfig base = new RuleConfig(10, always, new BoostConfig("multiplicative", 1.25));

    BoostSourceConfig original =
        new BoostSourceConfig(
            "modularjobs:mining_boost", "Enhanced mining", null, List.of(high, base));

    BoostSource parsed = parser.parse(original);
    BoostSourceConfig again = BoostSourceConfigSerializer.serialize(parsed);

    assertEquals(original.key(), again.key());
    assertEquals(2, again.rules().size());
    assertEquals(100, again.rules().get(0).priority());
    assertEquals("and", again.rules().get(0).conditions().type());
    assertEquals(2, again.rules().get(0).conditions().conditions().size());
    assertEquals("always", again.rules().get(1).conditions().type());
    assertTrue(again.rules().get(1).boost().amount() > 1.0);
  }
}
