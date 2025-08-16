package net.aincraft.boost.conditions;

import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.LogicalOperator;
import net.aincraft.api.container.boost.PlayerResourceType;
import net.aincraft.api.container.boost.RelationalOperator;
import net.aincraft.api.container.boost.factories.ConditionFactory;
import org.bukkit.World;
import org.bukkit.block.Biome;

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
    return null;
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
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    return new ComposableConditionImpl(a, b, operator);
  }
}
