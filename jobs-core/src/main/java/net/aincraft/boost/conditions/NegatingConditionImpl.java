package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

public record NegatingConditionImpl(Condition condition) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return !condition.applies(context);
  }

}
