package net.aincraft.serialization;

import net.aincraft.boost.conditions.NegatingConditionImpl;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record NegatingConditionCodecImpl() implements Codec.Typed<NegatingConditionImpl> {

  @Override
  public Class<NegatingConditionImpl> type() {
    return NegatingConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, NegatingConditionImpl condition, Writer writer) {
    writer.write(out, condition.condition());
  }

  @Override
  public NegatingConditionImpl decode(BinaryIn in, Reader reader) {
    return new NegatingConditionImpl(reader.read(in, Condition.class));
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:negate_condition");
  }
}
