package net.aincraft.serialization;

import net.aincraft.boost.conditions.ComposableConditionImpl;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record ComposableConditionCodecImpl() implements Codec.Typed<ComposableConditionImpl> {

  @Override
  public Class<ComposableConditionImpl> type() {
    return ComposableConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, ComposableConditionImpl condition, Writer writer) {
    out.writeEnum(condition.logicalOperator());
    writer.write(out, condition.a());
    writer.write(out, condition.b());
  }

  @Override
  public ComposableConditionImpl decode(BinaryIn in, Reader reader) {
    LogicalOperator operator = in.readEnum(LogicalOperator.class);
    Condition a = reader.read(in, Condition.class);
    Condition b = reader.read(in, Condition.class);
    return new ComposableConditionImpl(a, b, operator);
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:composable_condition");
  }
}
