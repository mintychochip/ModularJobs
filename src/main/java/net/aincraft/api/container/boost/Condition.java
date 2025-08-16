package net.aincraft.api.container.boost;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.boost.factories.ConditionFactory;
import org.bukkit.World;
import org.bukkit.block.Biome;

public interface Condition {

  ConditionFactory FACTORY = Bridge.bridge().conditionFactory();

  boolean applies(BoostContext context);

  static Condition biome(Biome biome) {
    return FACTORY.biome(biome);
  }

  static Condition world(World world) {
    return FACTORY.world(world);
  }

  static Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return FACTORY.playerResource(type, expected, operator);
  }

  static Condition sneaking(boolean state) {
    return FACTORY.sneaking(state);
  }

  static Condition sprinting(boolean state) {
    return FACTORY.sprinting(state);
  }

  default Condition and(Condition other) {
    return compose(other, LogicalOperator.AND);
  }

  default Condition or(Condition other) {
    return compose(other, LogicalOperator.OR);
  }

  default Condition negate() {
    return FACTORY.negate(this);
  }

  default Condition compose(Condition other, LogicalOperator operator) {
    return FACTORY.compose(this, other, operator);
  }
}
