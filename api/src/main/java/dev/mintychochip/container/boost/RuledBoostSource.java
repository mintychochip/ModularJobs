package dev.mintychochip.container.boost;

import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostSource;
import java.util.List;

/**
 * A {@link BoostSource} that resolves its output through a set of {@link Rule rules}. For a given
 * evaluation context, the matching rule with the highest {@link Rule#priority() priority}
 * determines the applied boost; if no rule matches, the source yields no boost.
 */
public interface RuledBoostSource extends BoostSource {

  /**
   * Returns the rules governing which boost applies.
   *
   * @return the rules of this source
   */
  List<Rule> rules();

  /**
   * A single rule binding a condition to a boost with a priority.
   *
   * @param condition condition that selects this rule
   * @param priority ordering weight; higher-priority rules take precedence
   * @param boost the boost to apply when the rule matches
   */
  record Rule(Condition condition, int priority, Boost boost) {}
}
