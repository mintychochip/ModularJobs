package net.aincraft.boost.policies;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.RuledBoostSource.Policy;
import net.aincraft.api.container.RuledBoostSource.Rule;

public class FirstMatchPolicyImpl implements Policy {

  @Override
  public List<Boost> resolve(Collection<Rule> rules, BoostContext context) {
    return List.of();
  }
}
