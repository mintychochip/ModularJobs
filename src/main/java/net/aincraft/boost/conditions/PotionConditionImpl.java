package net.aincraft.boost.conditions;

import java.math.BigDecimal;
import java.util.Collection;
import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.container.boost.PotionConditionType;
import net.aincraft.api.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

record PotionConditionImpl(PotionEffectType type,
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

  static final class CodecImpl implements Codec.Typed<PotionConditionImpl> {

    @Override
    public void encode(Out out, PotionConditionImpl object, Writer writer) {
      out.writeKey(object.type.key());
      out.writeInt(object.expected);
      out.writeEnum(object.conditionType);
      out.writeEnum(object.relationalOperator);
    }

    @Override
    public PotionConditionImpl decode(In in, Reader reader) {
      Key effectTypeKey = in.readKey();
      PotionEffectType potionEffectType = Registry.POTION_EFFECT_TYPE.get(effectTypeKey);
      int expected = in.readInt();
      PotionConditionType conditionType = in.readEnum(PotionConditionType.class);
      RelationalOperator relationalOperator = in.readEnum(RelationalOperator.class);
      return new PotionConditionImpl(potionEffectType, expected, conditionType, relationalOperator);
    }

    @Override
    public Class<?> type() {
      return PotionConditionImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:potion_condition");
    }
  }
}
