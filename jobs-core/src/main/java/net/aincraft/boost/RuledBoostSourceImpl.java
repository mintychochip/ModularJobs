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

public record RuledBoostSourceImpl(List<Rule> rules, Policy policy) implements RuledBoostSource {

  private static final Queue<Rule> RULE_QUEUE = new PriorityQueue<>(
      Comparator.comparingInt(Rule::priority).reversed());

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