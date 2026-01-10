package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Record condition that composes two conditions with a logical operator.
 * Delegates to {@link Conditions#compose(Condition, Condition, LogicalOperator)} for implementation.
 */
@Internal
public record ComposableConditionImpl(Condition a, Condition b,
                                      LogicalOperator logicalOperator) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return logicalOperator.test(a.applies(context), b.applies(context));
  }
}
