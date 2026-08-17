package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.boost.conditions.Conditions;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;

/**
 * Single instance of {@link BoostFactory} and {@link ConditionFactory} that delegates to
 * {@link net.aincraft.boost.conditions.Conditions} and the record boost implementations.
 * <p>
 * String keys without a namespace are normalized to {@code minecraft:} before being turned
 * into {@link Key} values.
 */
public final class BoostFactoryImpl implements BoostFactory, ConditionFactory {

  public static final BoostFactoryImpl INSTANCE = new BoostFactoryImpl();

  /** Singleton instance; construction is private to this class. */
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
    return Conditions.biome(toKey(biomeKey));
  }

  @Override
  public Condition world(String worldName) {
    return Conditions.world(toKey(worldName));
  }

  @Override
  public Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return Conditions.playerResource(type, expected, operator);
  }

  @Override
  public Condition sneaking(boolean state) {
    return Conditions.sneaking(state);
  }

  @Override
  public Condition sprinting(boolean state) {
    return Conditions.sprinting(state);
  }

  @Override
  public Condition negate(Condition condition) {
    return Conditions.negate(condition);
  }

  @Override
  public Condition liquid(String materialKey) throws IllegalArgumentException {
    return Conditions.liquid(materialKey);
  }

  @Override
  public Condition potionType(String potionEffectTypeKey) {
    return Conditions.potionType(toKey(potionEffectTypeKey));
  }

  @Override
  public Condition potion(String potionEffectTypeKey, int expected,
      PotionConditionType conditionType, RelationalOperator operator) {
    return Conditions.potion(toKey(potionEffectTypeKey), expected, conditionType, operator);
  }

  @Override
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    return Conditions.compose(a, b, operator);
  }

  @Override
  public Condition weather(WeatherState state) {
    return Conditions.weather(state);
  }

  @Override
  public Condition job(String jobKey) {
    return Conditions.job(jobKey);
  }

  @Override
  public Condition jobAny(String... jobKeys) {
    return Conditions.jobAny(jobKeys);
  }

  /**
   * Normalizes a raw key string into a {@link Key}: trims and lowercases it, prepending
   * the {@code minecraft:} namespace when no namespace separator is present.
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
}
