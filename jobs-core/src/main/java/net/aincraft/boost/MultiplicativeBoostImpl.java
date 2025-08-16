package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.container.Boost;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record MultiplicativeBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.multiply(amount);
  }

  static final class CodecImpl implements Codec.Typed<MultiplicativeBoostImpl> {

    @Override
    public void encode(Out out, MultiplicativeBoostImpl object, Writer writer) {
      out.writeBigDecimal(object.amount);
    }

    @Override
    public MultiplicativeBoostImpl decode(In in, Reader reader) {
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
}
