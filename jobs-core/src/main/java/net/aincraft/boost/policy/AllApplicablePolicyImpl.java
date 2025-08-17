package net.aincraft.boost.policy;

import java.util.List;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;

public record AllApplicablePolicyImpl() implements Policy {

  public static AllApplicablePolicyImpl INSTANCE = new AllApplicablePolicyImpl();

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    return rules.stream().map(Rule::boost).toList();
  }

}
