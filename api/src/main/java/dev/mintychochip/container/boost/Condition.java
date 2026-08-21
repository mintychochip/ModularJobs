package dev.mintychochip.container.boost;

import dev.mintychochip.Bridge;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.factories.ConditionFactory;

/**
 * Predicate evaluated against a {@link BoostContext}.
 */
public interface Condition {

  /**
   * Tests whether this condition applies to the supplied context.
   *
   * @param context context to evaluate
   * @return {@code true} when the condition applies
   */
  boolean applies(BoostContext context);

  /**
   * Lazy factory access — must not run at class-init time (unit tests load
   * {@link Condition} without a live Bukkit server / Bridge).
   */
  private static ConditionFactory factory() {
    return Bridge.bridge().conditionFactory();
  }

  /**
   * Creates a biome condition.
   *
   * @param biomeKey biome id or namespaced key (e.g. {@code plains}, {@code minecraft:desert})
   */
  static Condition biome(String biomeKey) {
    return factory().biome(biomeKey);
  }

  /**
   * Creates a world condition.
   *
   * @param worldName world name or namespaced key
   * @return a condition matching the world
   */
  static Condition world(String worldName) {
    return factory().world(worldName);
  }

  static Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return factory().playerResource(type, expected, operator);
  }

  static Condition sneaking(boolean state) {
    return factory().sneaking(state);
  }

  static Condition sprinting(boolean state) {
    return factory().sprinting(state);
  }

  /**
   * @param materialKey liquid material name or key ({@code water}/{@code lava})
   */
  static Condition liquid(String materialKey) throws IllegalArgumentException {
    return factory().liquid(materialKey);
  }

  /**
   * @param potionEffectTypeKey effect id or namespaced key (e.g. {@code speed}, {@code minecraft:strength})
   */
  static Condition potionType(String potionEffectTypeKey) {
    return factory().potionType(potionEffectTypeKey);
  }

  /**
   * @param potionEffectTypeKey effect id or namespaced key
   */
  static Condition potion(String potionEffectTypeKey, int expected,
      PotionConditionType conditionType, RelationalOperator operator) {
    return factory().potion(potionEffectTypeKey, expected, conditionType, operator);
  }

  static Condition weather(WeatherState state) {
    return factory().weather(state);
  }

  default Condition and(Condition other) {
    return compose(other, LogicalOperator.AND);
  }

  default Condition or(Condition other) {
    return compose(other, LogicalOperator.OR);
  }

  default Condition negate() {
    return factory().negate(this);
  }

  default Condition compose(Condition other, LogicalOperator operator) {
    return factory().compose(this, other, operator);
  }
}
