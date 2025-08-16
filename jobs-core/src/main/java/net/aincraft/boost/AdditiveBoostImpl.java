package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record AdditiveBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.add(this.amount);
  }

  static final class CodecImpl implements Codec.Typed<AdditiveBoostImpl> {

    @Override
    public void encode(Out out, AdditiveBoostImpl object, Writer writer) {
      out.writeBigDecimal(object.amount);
    }

    @Override
    public AdditiveBoostImpl decode(In in, Reader reader) {
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
}
