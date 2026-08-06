package net.aincraft.container.boost;

import java.util.function.Function;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;

public enum PlayerResourceType {
  HEALTH(Damageable::getHealth),
  HUNGER(player -> (double) player.getFoodLevel()),
  EXPERIENCE(player -> (double) player.getExp());
  private final Function<Player, Double> valueFunction;

  PlayerResourceType(Function<Player, Double> valueFunction) {
    this.valueFunction = valueFunction;
  }

  public Double getValue(Player player) {
    return valueFunction.apply(player);
  }
}
