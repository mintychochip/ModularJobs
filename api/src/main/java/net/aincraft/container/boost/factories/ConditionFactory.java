package net.aincraft.container.boost.factories;

import net.aincraft.Bridge;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface ConditionFactory {

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
   * @param worldName world name or namespaced key (resolved at evaluation)
   */
  Condition world(String worldName);

  Condition playerResource(PlayerResourceType type, double expected, RelationalOperator operator);

  Condition sneaking(boolean state);

  Condition sprinting(boolean state);

  Condition negate(Condition condition);

  /**
   * @param materialKey liquid material name or key ({@code water}/{@code lava})
   */
  Condition liquid(String materialKey) throws IllegalArgumentException;

  /**
   * @param potionEffectTypeKey effect id or namespaced key
   */
  Condition potionType(String potionEffectTypeKey);

  /**
   * @param potionEffectTypeKey effect id or namespaced key
   */
  Condition potion(String potionEffectTypeKey, int expected, PotionConditionType conditionType,
      RelationalOperator operator);

  Condition compose(Condition a, Condition b, LogicalOperator operator);

  Condition weather(WeatherState state);

  Condition job(String jobKey);

  Condition jobAny(String... jobKeys);
}
