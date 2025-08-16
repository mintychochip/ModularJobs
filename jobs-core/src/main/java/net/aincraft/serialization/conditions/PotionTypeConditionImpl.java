package net.aincraft.serialization.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

record PotionTypeConditionImpl(PotionEffectType type) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().hasPotionEffect(type);
  }

  static final class CodecImpl implements Codec.Typed<PotionTypeConditionImpl> {

    @Override
    public void encode(Out out, PotionTypeConditionImpl object, Writer writer) {
      out.writeKey(object.type.key());
    }

    @Override
    public PotionTypeConditionImpl decode(In in, Reader reader) {
      Key effectTypeKey = in.readKey();
      PotionEffectType potionEffectType = Registry.POTION_EFFECT_TYPE.get(effectTypeKey);
      return new PotionTypeConditionImpl(potionEffectType);
    }

    @Override
    public Class<?> type() {
      return PotionTypeConditionImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:potion_type_condition");
    }
  }
}
