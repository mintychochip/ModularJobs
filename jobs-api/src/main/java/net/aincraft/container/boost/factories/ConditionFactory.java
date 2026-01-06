package net.aincraft.container.boost.factories;

import net.aincraft.Bridge;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface ConditionFactory {

  static ConditionFactory conditionFactory() {
    return Bridge.bridge().conditionFactory();
  }

  Condition biome(Biome biome);

  Condition world(World world);

  Condition playerResource(PlayerResourceType type, double expected, RelationalOperator operator);

  Condition sneaking(boolean state);

  Condition sprinting(boolean state);

  Condition negate(Condition condition);

  Condition liquid(Material liquid) throws IllegalArgumentException;

  Condition potionType(PotionEffectType type);

  Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator operator);

  Condition compose(Condition a, Condition b, LogicalOperator operator);

  Condition weather(WeatherState state);

  Condition job(String jobKey);

  Condition jobAny(String... jobKeys);
}
