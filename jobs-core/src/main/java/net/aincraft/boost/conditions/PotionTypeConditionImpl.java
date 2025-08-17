package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import org.bukkit.potion.PotionEffectType;

public record PotionTypeConditionImpl(PotionEffectType type) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().hasPotionEffect(type);
  }

}
