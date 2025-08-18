package net.aincraft.serialization;

import java.time.Duration;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record ConsumableBoostDataCodecImpl() implements Codec.Typed<ConsumableBoostData> {

  @Override
  public void encode(BinaryOut out, ConsumableBoostData object, Writer writer) {
    writer.write(out,object.boostSource());
    writer.write(out,object.duration());
  }

  @Override
  public ConsumableBoostData decode(BinaryIn in, Reader reader) {
    BoostSource boostSource = reader.read(in, BoostSource.class);
    Duration duration = reader.read(in, Duration.class);
    return new ConsumableBoostData(boostSource,duration);
  }

  @Override
  public Class<?> type() {
    return ConsumableBoostData.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:consumable_boost_data");
  }
}
