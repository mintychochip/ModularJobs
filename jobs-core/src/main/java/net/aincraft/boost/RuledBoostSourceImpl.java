package net.aincraft.boost;

import java.util.List;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.RuledBoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record RuledBoostSourceImpl(List<Rule> rules, Key key, String description) implements RuledBoostSource {

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    // Return all applicable boosts (all rules whose conditions match)
    return rules.stream()
        .filter(rule -> rule.condition().applies(context))
        .map(Rule::boost)
        .toList();
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}