package net.aincraft.boost.conditions;

import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Consolidated static utility class for creating {@link Condition} instances.
 * All condition creation is centralized here, returning record implementations
 * that support Kryo serialization and pattern matching.
 */
public final class Conditions {

  private Conditions() {}

  /** Condition that applies when the player is in the biome identified by {@code biomeKey}. */
  public static Condition biome(Key biomeKey) {
    return new BiomeConditionImpl(biomeKey);
  }

  /** Condition that applies when the player is in the world identified by {@code worldKey}. */
  public static Condition world(Key worldKey) {
    return new WorldConditionImpl(worldKey);
  }

  /** Condition that applies when the player's sneaking state equals {@code state}. */
  public static Condition sneaking(boolean state) {
    return new SneakConditionImpl(state);
  }

  /** Condition that applies when the player's sprinting state equals {@code state}. */
  public static Condition sprinting(boolean state) {
    return new SprintConditionImpl(state);
  }

  /** Condition that applies when {@code condition} does not apply. */
  public static Condition negate(Condition condition) {
    return new NegatingConditionImpl(condition);
  }

  /**
   * @param materialKey liquid material name or key ({@code water}/{@code lava}); resolved at evaluation
   */
  public static Condition liquid(String materialKey) {
    return new LiquidConditionImpl(materialKey);
  }

  /** Condition that applies when the current weather equals {@code state}. */
  public static Condition weather(WeatherState state) {
    return new WeatherConditionImpl(state);
  }

  /** Condition that applies when the player has the potion effect identified by {@code effectKey}. */
  public static Condition potionType(Key effectKey) {
    return new PotionTypeConditionImpl(effectKey);
  }

  /**
   * Condition comparing a potion-effect dimension to an expected value.
   *
   * @param relationalOperator how {@code actual} compares to {@code expected}
   */
  public static Condition potion(Key effectKey, int expected, PotionConditionType conditionType,
      RelationalOperator relationalOperator) {
    return new PotionConditionImpl(effectKey, expected, conditionType, relationalOperator);
  }

  /**
   * Condition comparing a player resource (health/hunger/experience) to an expected value.
   */
  public static Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return new PlayerResourceConditionImpl(type, expected, operator);
  }

  /** Condition that combines {@code a} and {@code b} with a logical operator. */
  public static Condition compose(Condition a, Condition b, LogicalOperator logicalOperator) {
    return new ComposableConditionImpl(a, b, logicalOperator);
  }

  /** Condition that applies when the player's current job key is {@code jobKey}. */
  @Internal
  public static Condition job(String jobKey) {
    return new JobConditionImpl(jobKey);
  }

  /** Condition that applies when the player's current job key matches any of {@code jobKeys}. */
  @Internal
  public static Condition jobAny(String... jobKeys) {
    return new JobConditionImpl(jobKeys);
  }
}
