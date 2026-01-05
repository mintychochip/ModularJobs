package net.aincraft.container.boost;

import java.util.function.Function;
import org.bukkit.potion.PotionEffect;

public enum PotionConditionType {
  DURATION(PotionEffect::getDuration),
  AMPLIFIER(PotionEffect::getAmplifier);
  private final Function<PotionEffect, Integer> valueFunction;

  PotionConditionType(Function<PotionEffect, Integer> valueFunction) {
    this.valueFunction = valueFunction;
  }

  public Integer getValue(PotionEffect effect) {
    return valueFunction.apply(effect);
  }
}
