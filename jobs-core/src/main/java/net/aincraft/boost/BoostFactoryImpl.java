package net.aincraft.boost;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.boost.conditions.ComposableConditionImpl;
import net.aincraft.boost.conditions.JobConditionImpl;
import net.aincraft.boost.conditions.LiquidConditionImpl;
import net.aincraft.boost.conditions.NegatingConditionImpl;
import net.aincraft.boost.conditions.PlayerResourceConditionImpl;
import net.aincraft.boost.conditions.PotionConditionImpl;
import net.aincraft.boost.conditions.PotionTypeConditionImpl;
import net.aincraft.boost.conditions.SneakConditionImpl;
import net.aincraft.boost.conditions.SprintConditionImpl;
import net.aincraft.boost.conditions.WeatherConditionImpl;
import net.aincraft.boost.conditions.WorldConditionImpl;
import net.aincraft.boost.policy.AllApplicablePolicyImpl;
import net.aincraft.boost.policy.GetFirstPolicyImpl;
import net.aincraft.boost.policy.TopKPolicyImpl;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;

public final class BoostFactoryImpl implements BoostFactory, ConditionFactory, PolicyFactory {

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
    return new BiomeConditionImpl(biome.key());
  }

  @Override
  public Condition world(World world) {
    return new WorldConditionImpl(world.key());
  }

  @Override
  public Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return new PlayerResourceConditionImpl(type, expected, operator);
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
    return new PotionConditionImpl(type, expected, conditionType, operator);
  }

  @Override
  public Condition compose(Condition a, Condition b, LogicalOperator operator) {
    return new ComposableConditionImpl(a, b, operator);
  }

  @Override
  public Condition weather(WeatherState state) {
    return new WeatherConditionImpl(state);
  }

  @Override
  public Condition job(String jobKey) {
    return new JobConditionImpl(jobKey);
  }

  @Override
  public Condition jobAny(String... jobKeys) {
    return new JobConditionImpl(jobKeys);
  }

  @Override
  public Policy first() {
    return GetFirstPolicyImpl.INSTANCE;
  }

  @Override
  public Policy allApplicable() {
    return AllApplicablePolicyImpl.INSTANCE;
  }

  @Override
  public Policy topKBoosts(int k) {
    return new TopKPolicyImpl(k);
  }
}
