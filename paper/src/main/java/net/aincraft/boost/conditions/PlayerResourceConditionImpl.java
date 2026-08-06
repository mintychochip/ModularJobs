package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.RelationalOperator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Record condition that checks a player resource value against an expected value.
 * Delegates to {@link Conditions#playerResource(PlayerResourceType, double, RelationalOperator)} for implementation.
 */
public record PlayerResourceConditionImpl(PlayerResourceType type, double expected,
                                   RelationalOperator operator) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = Bukkit.getPlayer(context.playerId());
    if (player == null) {
      return false;
    }
    double actual = type.getValue(player);
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
  }
}
