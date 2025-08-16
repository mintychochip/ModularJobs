package net.aincraft.api.container.boost.factories;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.LogicalOperator;
import net.aincraft.api.container.boost.PlayerResourceType;
import net.aincraft.api.container.boost.RelationalOperator;
import org.bukkit.World;
import org.bukkit.block.Biome;
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

  Condition compose(Condition a, Condition b, LogicalOperator operator);
}
