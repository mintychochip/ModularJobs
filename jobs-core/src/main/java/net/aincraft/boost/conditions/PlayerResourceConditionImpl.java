package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.RelationalOperator;

public record PlayerResourceConditionImpl(PlayerResourceType type, double expected,
                                   RelationalOperator operator) implements
    Condition {

  @Override
  public boolean applies(BoostContext context) {
    double actual = type.getValue(context.player());
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
  }

}
