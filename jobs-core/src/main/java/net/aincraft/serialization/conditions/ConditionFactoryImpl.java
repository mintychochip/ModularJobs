package net.aincraft.serialization.conditions;

import com.google.common.base.Preconditions;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.boost.factories.ConditionFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

public final class ConditionFactoryImpl implements ConditionFactory {

  private ConditionFactoryImpl() {
  }

  public static final ConditionFactory INSTANCE = new ConditionFactoryImpl();

  @Override
  public Condition biome(Biome biome) {
    return new BiomeConditionImpl(biome.key());
  }

  @Override
  public Condition world(World world) {
    return new WorldConditionImpl(world.key());
  }

  @Override
  public Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return new PlayerResourceConditionImpl(type,expected,operator);
  }

  @Override
  public Condition sneaking(boolean state) {
    return new SneakConditionImpl(state);
  }

  @Override
  public Condition sprinting(boolean state) {
    return new SprintConditionImpl(state);
  }

  @Override
  public Condition negate(Condition condition) {
    return new NegatingConditionImpl(condition);
  }

  @Override
  public Condition liquid(Material liquid) throws IllegalArgumentException {
    Preconditions.checkArgument(liquid == Material.WATER || liquid == Material.LAVA);
    return new LiquidConditionImpl(liquid);
  }

  @Override
  public Condition potionType(PotionEffectType type) {
    return new PotionTypeConditionImpl(type);
  }

  @Override
  public Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator operator) {
    return new PotionConditionImpl(type,expected,conditionType,operator);
  }

  @Override
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    return new ComposableConditionImpl(a, b, operator);
  }

  @Override
  public Condition weather(WeatherState state) {
    return new WeatherConditionImpl(state);
  }
}
