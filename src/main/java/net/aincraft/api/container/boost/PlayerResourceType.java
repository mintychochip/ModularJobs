package net.aincraft.api.container.boost;

import java.util.function.Function;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;

public enum PlayerResourceType {
  HEALTH(Damageable::getHealth),
  HUNGER(player -> (double) player.getFoodLevel()),
  EXPERIENCE(player -> (double) player.getExp()),
  EXPERIENCE_LEVEL(player -> (double) player.getLevel());
  private final Function<Player, Double> valueFunction;

  PlayerResourceType(Function<Player, Double> valueFunction) {
    this.valueFunction = valueFunction;
  }

  Double apply(Player player) {
    return valueFunction.apply(player);
  }
}
