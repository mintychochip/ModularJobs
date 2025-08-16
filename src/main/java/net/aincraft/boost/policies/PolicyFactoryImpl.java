package net.aincraft.boost.policies;

import net.aincraft.api.container.PolicyFactory;
import net.aincraft.api.container.RuledBoostSource.Policy;

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
