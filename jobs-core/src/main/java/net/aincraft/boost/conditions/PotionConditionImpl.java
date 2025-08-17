package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import java.util.Collection;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public record PotionConditionImpl(PotionEffectType type,
                           int expected, PotionConditionType conditionType,
                           RelationalOperator relationalOperator) implements
    Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    Collection<PotionEffect> effects = player.getActivePotionEffects();
    for (PotionEffect effect : effects) {
      if (effect.getType() == type) {
        Integer actual = conditionType.getValue(effect);
        return relationalOperator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
      }
    }
    return false;
  }

}
