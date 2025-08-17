package net.aincraft.serialization;

import net.aincraft.boost.data.PassiveBoostDataImpl;
import net.aincraft.container.boost.RuledBoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record PassiveBoostDataCodecImpl() implements Codec.Typed<PassiveBoostDataImpl> {

  @Override
  public void encode(BinaryOut out, PassiveBoostDataImpl object, Writer writer) {
    writer.write(out, object.boostSource());
    writer.write(out, object.slots());
  }

  @Override
  public PassiveBoostDataImpl decode(BinaryIn in, Reader reader) {
    RuledBoostSource boostSource = reader.read(in, RuledBoostSource.class);
    int[] slots = reader.read(in, int[].class);
    return new PassiveBoostDataImpl(boostSource, slots);
  }

  @Override
  public Class<?> type() {
    return PassiveBoostDataImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:passive_boost_data");
  }
}
