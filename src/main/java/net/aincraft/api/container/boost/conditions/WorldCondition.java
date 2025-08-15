package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

public record WorldCondition(NamespacedKey worldKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return worldKey.equals(context.world().getKey());
  }
}
