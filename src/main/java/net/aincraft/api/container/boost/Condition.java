package net.aincraft.api.container.boost;

import java.math.BigDecimal;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.conditions.BiomeCondition;
import net.aincraft.api.container.boost.conditions.ComposableCondition;
import net.aincraft.api.container.boost.conditions.NegatingCondition;
import net.aincraft.api.container.boost.conditions.PlayerResourceCondition;
import net.aincraft.api.container.boost.conditions.PlayerResourceCondition.PlayerResourceType;
import net.aincraft.api.container.boost.conditions.SneakCondition;
import net.aincraft.api.container.boost.conditions.SprintCondition;
import net.aincraft.api.container.boost.conditions.WorldCondition;
import org.bukkit.World;
import org.bukkit.block.Biome;

public interface Condition {

  boolean applies(BoostContext context);

  static Condition biome(Biome biome) {
    return new BiomeCondition(biome.getKey());
  }

  static Condition world(World world) {
    return new WorldCondition(world.getKey());
  }

  static Condition playerResource(PlayerResourceType type, BigDecimal expected, RelationalOperator operator) {
    return new PlayerResourceCondition(type,expected,operator);
  }

  static Condition sneaking(boolean state) {
    return new SneakCondition(state);
  }

  static Condition sprinting(boolean state) {
    return new SprintCondition(state);
  }

  default Condition and(Condition other) {
    return compose(other, LogicalOperator.AND);
  }

  default Condition or(Condition other) {
    return compose(other, LogicalOperator.OR);
  }

  default Condition negate() {
    return new NegatingCondition(this);
  }

  default Condition compose(Condition other, LogicalOperator operator) {
    return new ComposableCondition(this, other, operator);
  }
}
