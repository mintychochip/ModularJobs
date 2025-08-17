package net.aincraft.serialization;

import net.aincraft.boost.conditions.SneakConditionImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record SneakConditionCodecImpl() implements Codec.Typed<SneakConditionImpl> {

  @Override
  public Class<SneakConditionImpl> type() {
    return SneakConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, SneakConditionImpl condition, Writer writer) {
    out.writeBool(condition.state());
  }

  @Override
  public SneakConditionImpl decode(BinaryIn in, Reader reader) {
    return new SneakConditionImpl(in.readBool());
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:sneak_condition");
  }
}
