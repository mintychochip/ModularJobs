package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

/**
 * Record condition that negates another condition.
 * Delegates to {@link Conditions#negate(Condition)} for implementation.
 */
public record NegatingConditionImpl(Condition condition) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return !condition.applies(context);
  }
}
