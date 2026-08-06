package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Record condition that checks if the player is sprinting.
 * Delegates to {@link Conditions#sprinting(boolean)} for implementation.
 */
public record SprintConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return false;
    }
    return player.isSprinting() == state;
  }
}
