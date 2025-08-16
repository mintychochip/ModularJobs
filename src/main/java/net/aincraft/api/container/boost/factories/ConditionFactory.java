package net.aincraft.api.container.boost.factories;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.WeatherState;
import net.aincraft.api.container.boost.LogicalOperator;
import net.aincraft.api.container.boost.PlayerResourceType;
import net.aincraft.api.container.boost.PotionConditionType;
import net.aincraft.api.container.boost.RelationalOperator;
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
}
