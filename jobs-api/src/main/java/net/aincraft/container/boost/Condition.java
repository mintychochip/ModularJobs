package net.aincraft.container.boost;

import net.aincraft.Bridge;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.factories.ConditionFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

public interface Condition {

  boolean applies(BoostContext context);

  /**
   * Lazy factory access — must not run at class-init time (unit tests load
   * {@link Condition} without a live Bukkit server / Bridge).
   */
  private static ConditionFactory factory() {
    return Bridge.bridge().conditionFactory();
  }

  static Condition biome(Biome biome) {
    return factory().biome(biome);
  }

  static Condition world(World world) {
    return factory().world(world);
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

  static Condition liquid(Material liquid) throws IllegalArgumentException {
    return factory().liquid(liquid);
  }

  static Condition potionType(PotionEffectType type) {
    return factory().potionType(type);
  }

  static Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator operator) {
    return factory().potion(type, expected, conditionType, operator);
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
