package dev.mintychochip.boost;

import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.PlayerResourceType;
import dev.mintychochip.container.boost.PotionConditionType;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.WeatherState;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.databag.Conditions;
import java.math.BigDecimal;
import net.kyori.adventure.key.Key;

/**
 * Boost and condition factory. Conditions are snapshot-graph types from {@code
 * dev.mintychochip.databag}, adapted onto the boost {@link Condition} interface.
 */
public final class BoostFactoryImpl implements BoostFactory, ConditionFactory {

  public static final BoostFactoryImpl INSTANCE = new BoostFactoryImpl();

  private BoostFactoryImpl() {}

  @Override
  public Boost additive(BigDecimal amount) {
    return new AdditiveBoostImpl(amount);
  }

  @Override
  public Boost multiplicative(BigDecimal amount) {
    return new MultiplicativeBoostImpl(amount);
  }

  @Override
  public Condition biome(String biomeKey) {
    return SnapshotCondition.wrap(Conditions.biome(toKey(biomeKey)));
  }

  @Override
  public Condition world(String worldName) {
    return SnapshotCondition.wrap(Conditions.world(worldName));
  }

  @Override
  public Condition playerResource(
      PlayerResourceType type, double expected, RelationalOperator operator) {
    return SnapshotCondition.wrap(
        Conditions.playerResource(mapResource(type), mapOperator(operator), expected));
  }

  @Override
  public Condition sneaking(boolean state) {
    return SnapshotCondition.wrap(Conditions.sneaking(state));
  }

  @Override
  public Condition sprinting(boolean state) {
    return SnapshotCondition.wrap(Conditions.sprinting(state));
  }

  @Override
  public Condition negate(Condition condition) {
    return SnapshotCondition.wrap(Conditions.inverted(SnapshotCondition.unwrap(condition)));
  }

  @Override
  public Condition liquid(String materialKey) {
    return SnapshotCondition.wrap(Conditions.fluid(toKey(materialKey)));
  }

  @Override
  public Condition potionType(String potionEffectTypeKey) {
    return SnapshotCondition.wrap(Conditions.potionPresent(toKey(potionEffectTypeKey)));
  }

  @Override
  public Condition potion(
      String potionEffectTypeKey,
      int expected,
      PotionConditionType conditionType,
      RelationalOperator operator) {
    Key key = toKey(potionEffectTypeKey);
    dev.mintychochip.databag.RelationalOperator op = mapOperator(operator);
    return SnapshotCondition.wrap(
        switch (conditionType) {
          case AMPLIFIER -> Conditions.potionAmplifier(key, op, expected);
          case DURATION -> Conditions.potionDuration(key, op, expected);
        });
  }

  @Override
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    dev.mintychochip.databag.Condition left = SnapshotCondition.unwrap(a);
    dev.mintychochip.databag.Condition right = SnapshotCondition.unwrap(b);
    dev.mintychochip.databag.Condition composed =
        switch (operator) {
          case AND -> Conditions.allOf(left, right);
          case OR -> Conditions.anyOf(left, right);
          default -> ctx -> operator.test(left.test(ctx), right.test(ctx));
        };
    return SnapshotCondition.wrap(composed);
  }

  @Override
  public Condition weather(WeatherState state) {
    return SnapshotCondition.wrap(Conditions.weather(mapWeather(state)));
  }

  @Override
  public Condition job(String jobKey) {
    return SnapshotCondition.wrap(Conditions.job(jobKey));
  }

  @Override
  public Condition jobAny(String... jobKeys) {
    return SnapshotCondition.wrap(Conditions.jobAny(jobKeys));
  }

  /**
   * Normalizes a raw key string into a {@link Key}: trims and lowercases it, prepending the {@code
   * minecraft:} namespace when no namespace separator is present.
   *
   * @param raw the raw key string
   * @return the normalized {@link Key}
   * @throws IllegalArgumentException if {@code raw} is null or blank
   */
  private static Key toKey(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("key must be non-blank");
    }
    String trimmed = raw.trim();
    if (trimmed.contains(":")) {
      return Key.key(trimmed.toLowerCase());
    }
    return Key.key("minecraft", trimmed.toLowerCase());
  }

  private static dev.mintychochip.databag.PlayerResourceType mapResource(PlayerResourceType type) {
    return switch (type) {
      case HEALTH -> dev.mintychochip.databag.PlayerResourceType.HEALTH;
      case HUNGER -> dev.mintychochip.databag.PlayerResourceType.HUNGER;
      case EXPERIENCE -> dev.mintychochip.databag.PlayerResourceType.EXPERIENCE;
    };
  }

  private static dev.mintychochip.databag.RelationalOperator mapOperator(
      RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> dev.mintychochip.databag.RelationalOperator.LESS_THAN;
      case LESS_THAN_OR_EQUAL -> dev.mintychochip.databag.RelationalOperator.LESS_THAN_OR_EQUAL;
      case GREATER_THAN -> dev.mintychochip.databag.RelationalOperator.GREATER_THAN;
      case GREATER_THAN_OR_EQUAL ->
          dev.mintychochip.databag.RelationalOperator.GREATER_THAN_OR_EQUAL;
      case EQUAL -> dev.mintychochip.databag.RelationalOperator.EQUAL;
      case NOT_EQUAL -> dev.mintychochip.databag.RelationalOperator.NOT_EQUAL;
    };
  }

  private static dev.mintychochip.databag.WeatherState mapWeather(WeatherState state) {
    return switch (state) {
      case THUNDERING -> dev.mintychochip.databag.WeatherState.THUNDERING;
      case RAINING -> dev.mintychochip.databag.WeatherState.RAINING;
      case CLEAR -> dev.mintychochip.databag.WeatherState.CLEAR;
    };
  }
}
