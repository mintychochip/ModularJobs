package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.bukkit.potion.PotionEffectType;

/**
 * Record condition that checks if the player has a specific potion effect.
 * Delegates to {@link Conditions#potionType(PotionEffectType)} for implementation.
 */
public record PotionTypeConditionImpl(PotionEffectType type) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().hasPotionEffect(type);
  }
}
