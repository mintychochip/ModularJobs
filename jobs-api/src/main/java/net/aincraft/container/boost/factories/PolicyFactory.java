package net.aincraft.container.boost.factories;

import net.aincraft.Bridge;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface PolicyFactory {

  static PolicyFactory policyFactory() {
    return Bridge.bridge().policyFactory();
  }

  Policy first();

  Policy allApplicable();

  Policy topKBoosts(int k);
}
