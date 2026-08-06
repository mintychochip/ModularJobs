package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

/**
 * Condition that always applies. Used for base rules without restrictions.
 */
public record AlwaysTrueConditionImpl() implements Condition {

  public static final AlwaysTrueConditionImpl INSTANCE = new AlwaysTrueConditionImpl();

  @Override
  public boolean applies(BoostContext context) {
    return true;
  }
}
