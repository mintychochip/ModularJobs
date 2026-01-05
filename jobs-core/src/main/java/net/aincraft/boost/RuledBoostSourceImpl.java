package net.aincraft.boost;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.RuledBoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record RuledBoostSourceImpl(List<Rule> rules, Policy policy, Key key) implements RuledBoostSource {

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    Queue<Rule> ruleQueue = new PriorityQueue<>(Comparator.comparingInt(Rule::priority).reversed());
    rules.stream().filter(rule -> rule.condition().applies(context)).forEach(ruleQueue::add);
    return policy.resolve(ruleQueue);
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}