package net.aincraft.container.boost.factories;

import net.aincraft.Bridge;
import net.aincraft.container.RuledBoostSource.Policy;

public interface PolicyFactory {

  static PolicyFactory policyFactory() {
    return Bridge.bridge().policyFactory();
  }

  Policy getFirst();

  Policy getAllApplicable();

  Policy getTopNBoosts(int n);
}
