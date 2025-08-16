package net.aincraft.api.container.boost;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.boost.factories.ConditionFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

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

  static Condition liquid(Material liquid) throws IllegalArgumentException {
    return FACTORY.liquid(liquid);
  }

  static Condition potionType(PotionEffectType type) {
    return FACTORY.potionType(type);
  }

  static Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator operator) {
    return FACTORY.potion(type, expected, conditionType, operator);
  }

  static Condition weather(WeatherState state) {
    return FACTORY.weather(state);
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
