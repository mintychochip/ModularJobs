package net.aincraft.serialization;

import java.time.Duration;
import net.aincraft.boost.data.ConsumableBoostDataBoostDataImpl;
import net.aincraft.container.boost.RuledBoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record ConsumableBoostDataCodecImpl() implements Codec.Typed<ConsumableBoostDataBoostDataImpl> {

  @Override
  public void encode(BinaryOut out, ConsumableBoostDataBoostDataImpl object, Writer writer) {
    writer.write(out, object.boostSource());
    writer.write(out, object.duration());
  }

  @Override
  public ConsumableBoostDataBoostDataImpl decode(BinaryIn in, Reader reader) {
    RuledBoostSource boostSource = reader.read(in, RuledBoostSource.class);
    Duration duration = reader.read(in, Duration.class);
    return new ConsumableBoostDataBoostDataImpl(boostSource, duration);
  }

  @Override
  public Class<?> type() {
    return ConsumableBoostDataBoostDataImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:consumable_boost_data");
  }
}
