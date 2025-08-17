package net.aincraft.serialization;

import net.aincraft.boost.conditions.LiquidConditionImpl;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public record LiquidConditionCodecImpl() implements Codec.Typed<LiquidConditionImpl> {

  @Override
  public void encode(BinaryOut out, LiquidConditionImpl object, Writer writer) {
    out.writeEnum(object.liquid());
  }

  @Override
  public LiquidConditionImpl decode(BinaryIn in, Reader reader) {
    return new LiquidConditionImpl(in.readEnum(Material.class));
  }

  @Override
  public Class<?> type() {
    return LiquidConditionImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:liquid_condition");
  }
}
