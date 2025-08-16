package net.aincraft.serialization.data;

import java.time.Duration;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record ConsumableBoostDataImpl(RuledBoostSource boostSource, Duration duration) implements
    ItemBoostData.Consumable {

  @Override
  public Duration getDuration() {
    return duration;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

  static final class CodecImpl implements Codec.Typed<ConsumableBoostDataImpl> {

    @Override
    public void encode(Out out, ConsumableBoostDataImpl object, Writer writer) {
      writer.write(out, object.boostSource);
      writer.write(out, object.duration);
    }

    @Override
    public ConsumableBoostDataImpl decode(In in, Reader reader) {
      RuledBoostSource boostSource = reader.read(in, RuledBoostSource.class);
      Duration duration = reader.read(in, Duration.class);
      return new ConsumableBoostDataImpl(boostSource, duration);
    }

    @Override
    public Class<?> type() {
      return ConsumableBoostDataImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:consumable_boost_data");
    }
  }
}
