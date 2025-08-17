package net.aincraft.serialization;

import net.aincraft.boost.data.CombinedBoostDataImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record CombinedBoostDataCodecImpl() implements Codec.Typed<CombinedBoostDataImpl> {

  @Override
  public void encode(BinaryOut out, CombinedBoostDataImpl object, Writer writer) {
    writer.write(out, object.boostSource());
    writer.write(out, object.duration());
    writer.write(out, object.slots());
  }

  @Override
  public CombinedBoostDataImpl decode(BinaryIn in, Reader reader) {
    return null;
  }

  @Override
  public Class<?> type() {
    return null;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:combined_boost_data");
  }
}
