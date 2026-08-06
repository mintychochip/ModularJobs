package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.RelationalOperator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Checks a player resource value against an expected value.
 * Resource extraction is Paper-side (API enum is pure).
 */
public record PlayerResourceConditionImpl(PlayerResourceType type, double expected,
                                   RelationalOperator operator) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return false;
    }
    double actual = switch (type) {
      case HEALTH -> player.getHealth();
      case HUNGER -> player.getFoodLevel();
      case EXPERIENCE -> player.getExp();
    };
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
  }
}
