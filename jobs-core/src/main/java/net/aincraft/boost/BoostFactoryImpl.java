package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.boost.conditions.Conditions;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

public final class BoostFactoryImpl implements BoostFactory, ConditionFactory {

  public static final BoostFactoryImpl INSTANCE = new BoostFactoryImpl();

  private BoostFactoryImpl() {}

  @Override
  public Boost additive(BigDecimal amount) {
    return new AdditiveBoostImpl(amount);
  }

  @Override
  public Boost multiplicative(BigDecimal amount) {
    return new MultiplicativeBoostImpl(amount);
  }

  @Override
  public Condition biome(Biome biome) {
    return Conditions.biome(biome.key());
  }

  @Override
  public Condition world(World world) {
    return Conditions.world(world.key());
  }

  @Override
  public Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return Conditions.playerResource(type, expected, operator);
  }

  @Override
  public Condition sneaking(boolean state) {
    return Conditions.sneaking(state);
  }

  @Override
  public Condition sprinting(boolean state) {
    return Conditions.sprinting(state);
  }

  @Override
  public Condition negate(Condition condition) {
    return Conditions.negate(condition);
  }

  @Override
  public Condition liquid(Material liquid) throws IllegalArgumentException {
    return Conditions.liquid(liquid);
  }

  @Override
  public Condition potionType(PotionEffectType type) {
    return Conditions.potionType(type);
  }

  @Override
  public Condition potion(PotionEffectType type, int expected, PotionConditionType conditionType,
      RelationalOperator operator) {
    return Conditions.potion(type, expected, conditionType, operator);
  }

  @Override
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    return Conditions.compose(a, b, operator);
  }

  @Override
  public Condition weather(WeatherState state) {
    return Conditions.weather(state);
  }

  @Override
  public Condition job(String jobKey) {
    return Conditions.job(jobKey);
  }

  @Override
  public Condition jobAny(String... jobKeys) {
    return Conditions.jobAny(jobKeys);
  }
}
