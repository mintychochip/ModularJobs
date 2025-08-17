package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;

public record WorldConditionImpl(Key worldKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return worldKey.equals(context.world().getKey());
  }

}
