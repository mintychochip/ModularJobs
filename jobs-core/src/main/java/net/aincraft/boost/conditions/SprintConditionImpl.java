package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

/**
 * Record condition that checks if the player is sprinting.
 * Delegates to {@link Conditions#sprinting(boolean)} for implementation.
 */
public record SprintConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSprinting() == state;
  }
}
