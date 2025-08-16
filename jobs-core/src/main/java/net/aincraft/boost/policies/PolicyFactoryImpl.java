package net.aincraft.boost.policies;

import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.boost.factories.PolicyFactory;
public class PolicyFactoryImpl implements PolicyFactory {

  public static final PolicyFactory INSTANCE = new PolicyFactoryImpl();

  private PolicyFactoryImpl() {}
  @Override
  public Policy getFirst() {
    return GetFirstPolicyImpl.INSTANCE;
  }

  @Override
  public Policy getAllApplicable() {
    return AllApplicablePolicyImpl.INSTANCE;
  }

  @Override
  public Policy getTopNBoosts(int n) {
    return new TopNPolicyImpl(n);
  }
}
