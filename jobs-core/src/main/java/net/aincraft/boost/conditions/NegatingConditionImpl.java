package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record NegatingConditionImpl(Condition condition) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return !condition.applies(context);
  }

  static final class CodecImpl implements Codec.Typed<NegatingConditionImpl> {

    @Override
    public Class<NegatingConditionImpl> type() {
      return NegatingConditionImpl.class;
    }

    @Override
    public void encode(Out out, NegatingConditionImpl condition, Writer writer) {
      writer.write(out, condition.condition);
    }

    @Override
    public NegatingConditionImpl decode(In in, Reader reader) {
      return new NegatingConditionImpl(reader.read(in, Condition.class));
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:negate_condition");
    }
  }
}
