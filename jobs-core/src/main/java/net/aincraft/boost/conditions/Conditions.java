package net.aincraft.boost.conditions;

import com.google.common.base.Preconditions;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Consolidated static utility class for creating {@link Condition} instances.
 * All condition creation is centralized here, returning record implementations
 * that support Kryo serialization and pattern matching.
 */
public final class Conditions {

  private Conditions() {}

  public static Condition biome(Key biomeKey) {
    return new BiomeConditionImpl(biomeKey);
  }

  public static Condition world(Key worldKey) {
    return new WorldConditionImpl(worldKey);
  }

  public static Condition sneaking(boolean state) {
    return new SneakConditionImpl(state);
  }

  public static Condition sprinting(boolean state) {
    return new SprintConditionImpl(state);
  }

  public static Condition negate(Condition condition) {
    return new NegatingConditionImpl(condition);
  }

  public static Condition liquid(Material liquid) {
    return new LiquidConditionImpl(liquid);
  }

  public static Condition weather(WeatherState state) {
    return new WeatherConditionImpl(state);
  }

  public static Condition potionType(PotionEffectType type) {
    return new PotionTypeConditionImpl(type);
  }

  public static Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator relationalOperator) {
    return new PotionConditionImpl(type, expected, conditionType, relationalOperator);
  }

  public static Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return new PlayerResourceConditionImpl(type, expected, operator);
  }

  public static Condition compose(Condition a, Condition b, LogicalOperator logicalOperator) {
    return new ComposableConditionImpl(a, b, logicalOperator);
  }

  @Internal
  public static Condition job(String jobKey) {
    return new JobConditionImpl(jobKey);
  }

  @Internal
  public static Condition jobAny(String... jobKeys) {
    return new JobConditionImpl(jobKeys);
  }
}
