package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.LogicalOperator;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ComposableCondition(Condition a, Condition b,
                                  LogicalOperator logicalOperator) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return logicalOperator.test(a.applies(context), b.applies(context));
  }
}
