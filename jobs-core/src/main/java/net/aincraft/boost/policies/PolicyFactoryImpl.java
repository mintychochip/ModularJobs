package net.aincraft.boost.policies;

import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.kyori.adventure.key.Key;
public class PolicyFactoryImpl implements PolicyFactory {

  public static final PolicyFactory INSTANCE = new PolicyFactoryImpl();

  private PolicyFactoryImpl() {}
  @Override
  public Policy getFirst() {
    return GetFirstPolicyImpl.INSTANCE;
  }

  @Override
  public Policy allApplicable() {
    return AllApplicablePolicyImpl.INSTANCE;
  }

  @Override
  public Policy topN(int n) {
    return new TopNPolicyImpl(n);
  }
}
