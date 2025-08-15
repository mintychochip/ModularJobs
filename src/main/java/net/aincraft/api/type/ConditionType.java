package net.aincraft.api.type;

import net.aincraft.api.container.boost.Condition;

public interface ConditionType extends Type {
  Condition create();
}
