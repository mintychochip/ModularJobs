package net.aincraft.api.container.boost.conditions;

import java.math.BigDecimal;
import java.util.function.Function;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.RelationalOperator;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;

public record PlayerResourceCondition(PlayerResourceType type, BigDecimal expected,
                                      RelationalOperator relationalOperator) implements
    Condition {

  public enum PlayerResourceType {
    HEALTH(Damageable::getHealth),
    HUNGER(player -> (double) player.getFoodLevel()),
    EXPERIENCE(player -> (double) player.getExp()),
    EXPERIENCE_LEVEL(player -> (double) player.getLevel());
    private final Function<Player, Double> valueFunction;

    PlayerResourceType(Function<Player, Double> valueFunction) {
      this.valueFunction = valueFunction;
    }

    BigDecimal apply(Player player) {
      return BigDecimal.valueOf(valueFunction.apply(player));
    }
  }

  @Override
  public boolean applies(BoostContext context) {
    double actual = switch (type) {
      case HEALTH -> context.player().getHealth();
      case HUNGER -> context.player().getFoodLevel();
      case EXPERIENCE -> context.player().getExp();
      case EXPERIENCE_LEVEL -> context.player().getLevel();
    };
    return relationalOperator.test(BigDecimal.valueOf(actual),
        expected);
  }
}
