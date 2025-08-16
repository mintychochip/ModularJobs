package net.aincraft.api.container;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public final class RuledBoostSourceImpl implements RuledBoostSource {

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
    PriorityQueue<Rule> ruleQueue = new PriorityQueue<>(rules.size(),
        Comparator.comparingInt(Rule::priority).reversed());
    ruleQueue.addAll(rules);
    return policy.resolve(ruleQueue);
  }

  @Override
  public @NotNull Key key() {
    return null;
  }
}
