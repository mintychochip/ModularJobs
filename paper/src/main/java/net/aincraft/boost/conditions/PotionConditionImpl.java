package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import java.util.Collection;
import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Checks a potion effect value (e.g. amplifier) against an expected value.
 * Effect identity is a key so config can be parsed without a live potion registry.
 */
public record PotionConditionImpl(
    Key effectKey,
    int expected,
    PotionConditionType conditionType,
    RelationalOperator relationalOperator) implements Condition {

  public PotionConditionImpl(
      PotionEffectType type,
      int expected,
      PotionConditionType conditionType,
      RelationalOperator relationalOperator) {
    this(type.getKey(), expected, conditionType, relationalOperator);
  }

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    Collection<PotionEffect> effects = player.getActivePotionEffects();
    for (PotionEffect effect : effects) {
      if (matches(effect.getType())) {
        Integer actual = conditionType.getValue(effect);
        return relationalOperator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
      }
    }
    return false;
  }

  private boolean matches(PotionEffectType type) {
    Key key = type.getKey();
    return effectKey.equals(key)
        || effectKey.value().equalsIgnoreCase(key.value())
        || effectKey.asString().equalsIgnoreCase(key.asString());
  }
}
