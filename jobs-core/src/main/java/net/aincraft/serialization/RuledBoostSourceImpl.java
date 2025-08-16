package net.aincraft.serialization;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.RuledBoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class RuledBoostSourceImpl implements RuledBoostSource {

  private static final Queue<Rule> RULE_QUEUE = new PriorityQueue<>(
      Comparator.comparingInt(Rule::priority).reversed());

  private final Policy policy;
  private final List<Rule> rules;

  public RuledBoostSourceImpl(Policy policy, List<Rule> rules) {
    this.policy = policy;
    this.rules = rules;
  }

  @Override
  public Collection<Rule> rules() {
    return rules;
  }

  @Override
  public Policy policy() {
    return policy;
  }

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    RULE_QUEUE.clear();
    rules.stream().filter(rule -> rule.condition().applies(context)).forEach(RULE_QUEUE::add);
    return policy.resolve(RULE_QUEUE);
  }

  @Override
  public @NotNull Key key() {
    return Key.key("");
  }
}
