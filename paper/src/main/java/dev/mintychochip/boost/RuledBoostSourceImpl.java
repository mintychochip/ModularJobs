package dev.mintychochip.boost;

import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.RuledBoostSource;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Ruled boost source: among rules whose conditions match, only the single highest-{@link
 * Rule#priority() priority} rule's boost is returned. Lower-priority matching rules do not stack.
 */
public record RuledBoostSourceImpl(List<Rule> rules, Key key, String description)
    implements RuledBoostSource {

  @Override
  public @NotNull List<Boost> evaluate(BoostContext context) {
    Optional<Rule> winner =
        rules.stream()
            .filter(rule -> rule.condition().applies(context))
            .max(Comparator.comparingInt(Rule::priority));
    return winner.map(rule -> List.of(rule.boost())).orElseGet(List::of);
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}
