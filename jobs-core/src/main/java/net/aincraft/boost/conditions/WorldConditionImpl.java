package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;

/**
 * Record condition that checks if the player is in a specific world.
 * Delegates to {@link Conditions#world(Key)} for implementation.
 */
public record WorldConditionImpl(Key worldKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return worldKey.equals(context.world().getKey());
  }
}
