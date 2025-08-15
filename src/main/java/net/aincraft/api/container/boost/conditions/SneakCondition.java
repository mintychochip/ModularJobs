package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;

public record SneakCondition(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSneaking() == state;
  }
}
