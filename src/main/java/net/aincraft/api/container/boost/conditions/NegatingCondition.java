package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;

public record NegatingCondition(Condition condition) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return !condition.applies(context);
  }
}
