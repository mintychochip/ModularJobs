package net.aincraft.serialization;

import java.time.Duration;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record DurationCodecImpl() implements Codec.Typed<Duration> {

  @Override
  public void encode(BinaryOut out, Duration object, Writer writer) {
    out.writeLong(object.getSeconds());
    out.writeInt(object.getNano());
  }

  @Override
  public Duration decode(BinaryIn in, Reader reader) {
    long seconds = in.readLong();
    int nanos = in.readInt();
    return Duration.ofSeconds(seconds, nanos);
  }

  @Override
  public Class<?> type() {
    return Duration.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:duration");
  }
}
