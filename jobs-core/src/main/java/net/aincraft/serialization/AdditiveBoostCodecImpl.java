package net.aincraft.serialization;

import net.aincraft.boost.AdditiveBoostImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class AdditiveBoostCodecImpl implements Codec.Typed<AdditiveBoostImpl> {

  @Override
  public void encode(BinaryOut out, AdditiveBoostImpl object, Writer writer) {
    out.writeBigDecimal(object.amount());
  }

  @Override
  public AdditiveBoostImpl decode(BinaryIn in, Reader reader) {
    return new AdditiveBoostImpl(in.readBigDecimal());
  }

  @Override
  public Class<?> type() {
    return AdditiveBoostImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:additive_boost");
  }
}
