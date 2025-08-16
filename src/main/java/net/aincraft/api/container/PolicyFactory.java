package net.aincraft.api.container;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.RuledBoostSource.Policy;

public interface PolicyFactory {

  static PolicyFactory policyFactory() {
    return Bridge.bridge().policyFactory();
  }

  Policy getFirst();

  Policy allApplicable();

  Policy topN(int n);
}
