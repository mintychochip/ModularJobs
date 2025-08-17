package net.aincraft.boost.policy;

import java.util.List;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;

public record TopKPolicyImpl(int k) implements Policy {

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    return rules.stream().map(Rule::boost).limit(k).toList();
  }

}
