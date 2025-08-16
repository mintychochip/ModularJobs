package net.aincraft.serialization.data;

import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record PassiveBoostDataImpl(RuledBoostSource boostSource, int[] slots) implements
    ItemBoostData.Passive {

  @Override
  public int[] getApplicableSlots() {
    return slots;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

  static final class CodecImpl implements Codec.Typed<PassiveBoostDataImpl> {

    @Override
    public void encode(Out out, PassiveBoostDataImpl object, Writer writer) {
      writer.write(out, object.boostSource);
      writer.write(out, object.slots);
    }

    @Override
    public PassiveBoostDataImpl decode(In in, Reader reader) {
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
}
