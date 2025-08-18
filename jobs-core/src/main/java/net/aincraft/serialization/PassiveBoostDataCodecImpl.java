package net.aincraft.serialization;

import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record PassiveBoostDataCodecImpl() implements Codec.Typed<PassiveBoostData> {

  @Override
  public void encode(BinaryOut out, PassiveBoostData object, Writer writer) {
    writer.write(out,object.boostSource());
    writer.write(out,object.slotSet());
  }

  @Override
  public PassiveBoostData decode(BinaryIn in, Reader reader) {
    BoostSource boostSource = reader.read(in, BoostSource.class);
    SlotSet slotSet = reader.read(in, SlotSet.class);
    return new PassiveBoostData(boostSource,slotSet);
  }

  @Override
  public Class<?> type() {
    return PassiveBoostData.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:passive_boost_data");
  }
}
