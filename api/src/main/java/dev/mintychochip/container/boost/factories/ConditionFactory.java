package dev.mintychochip.container.boost.factories;

import dev.mintychochip.Bridge;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.PlayerResourceType;
import dev.mintychochip.container.boost.PotionConditionType;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.WeatherState;
import org.jetbrains.annotations.ApiStatus.Internal;

/** Type. */
@Internal
public interface ConditionFactory {

  /** Condition factory. */
  static ConditionFactory conditionFactory() {
    return Bridge.bridge().conditionFactory();
  }

  /**
   * Creates a biome condition.
   *
   * @param biomeKey biome id or namespaced key (resolved at evaluation)
   */
  Condition biome(String biomeKey);

  /**
   * Creates a world condition.
   *
   * @param worldName world name or namespaced key (resolved at evaluation)
   */
  Condition world(String worldName);

  /** Player resource. */
  Condition playerResource(PlayerResourceType type, double expected, RelationalOperator operator);

  /** Sneaking. */
  Condition sneaking(boolean state);

  /** Sprinting. */
  Condition sprinting(boolean state);

  /** Negate. */
  Condition negate(Condition condition);

  /**
   * Creates a liquid condition.
   *
   * @param materialKey liquid material name or key ({@code water}/{@code lava})
   */
  Condition liquid(String materialKey);

  /**
   * Creates a potion type condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key
   */
  Condition potionType(String potionEffectTypeKey);

  /**
   * Creates a potion intensity condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key
   */
  Condition potion(
      String potionEffectTypeKey,
      int expected,
      PotionConditionType conditionType,
      RelationalOperator operator);

  /** Compose. */
  Condition compose(Condition a, Condition b, LogicalOperator operator);

  /** Weather. */
  Condition weather(WeatherState state);

  /** Job. */
  Condition job(String jobKey);

  /** Job any. */
  Condition jobAny(String... jobKeys);
}
