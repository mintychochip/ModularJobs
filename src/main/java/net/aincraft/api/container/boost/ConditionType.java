package net.aincraft.api.container.boost;

import net.kyori.adventure.key.Keyed;

public interface ConditionType extends Keyed {
  ConditionAdapter adapter();
}
