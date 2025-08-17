package net.aincraft.serialization;

import net.aincraft.boost.MultiplicativeBoostImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record MultiplicativeBoostCodecImpl() implements Codec.Typed<MultiplicativeBoostImpl> {

  @Override
  public void encode(BinaryOut out, MultiplicativeBoostImpl object, Writer writer) {
    out.writeBigDecimal(object.amount());
  }

  @Override
  public MultiplicativeBoostImpl decode(BinaryIn in, Reader reader) {
    return new MultiplicativeBoostImpl(in.readBigDecimal());
  }

  @Override
  public Class<?> type() {
    return MultiplicativeBoostImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:multiplicative_boost");
  }
}
