package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;

/**
 * Record condition that checks if the player is sneaking.
 * Delegates to {@link Conditions#sneaking(boolean)} for implementation.
 */
public record SneakConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSneaking() == state;
  }
}
