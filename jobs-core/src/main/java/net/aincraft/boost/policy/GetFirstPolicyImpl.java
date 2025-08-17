package net.aincraft.boost.policy;

import java.util.List;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;

public record GetFirstPolicyImpl() implements Policy {

  public static final GetFirstPolicyImpl INSTANCE = new GetFirstPolicyImpl();

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    if (rules.isEmpty()) {
      return List.of();
    }
    Rule rule = rules.poll();
    return List.of(rule.boost());
  }

}
