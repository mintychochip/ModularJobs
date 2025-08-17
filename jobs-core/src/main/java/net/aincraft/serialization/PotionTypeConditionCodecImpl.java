package net.aincraft.serialization;

import net.aincraft.boost.conditions.PotionTypeConditionImpl;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public record PotionTypeConditionCodecImpl() implements Codec.Typed<PotionTypeConditionImpl> {

  @Override
  public void encode(BinaryOut out, PotionTypeConditionImpl object, Writer writer) {
    out.writeKey(object.type().key());
  }

  @Override
  public PotionTypeConditionImpl decode(BinaryIn in, Reader reader) {
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
