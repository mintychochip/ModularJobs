package net.aincraft.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.LogicalOperator;
import net.aincraft.api.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public record ComposableConditionImpl(Condition a, Condition b,
                                      LogicalOperator logicalOperator) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return logicalOperator.test(a.applies(context), b.applies(context));
  }

  static final class CodecImpl implements Codec.Typed<ComposableConditionImpl> {

    @Override
    public Class<ComposableConditionImpl> type() {
      return ComposableConditionImpl.class;
    }

    @Override
    public void encode(Out out, ComposableConditionImpl condition, Writer writer) {
      out.writeEnum(condition.logicalOperator);
      writer.write(out,condition.a);
      writer.write(out,condition.b);
    }

    @Override
    public Condition decode(In in, Reader reader) {
      LogicalOperator operator = in.readEnum(LogicalOperator.class);
      Condition a = reader.read(in);
      Condition b = reader.read(in);
      return new ComposableConditionImpl(a,b,operator);
    }

    @Override
    public @NotNull Key key() {
      return Key.key("conditions:composable");
    }
  }
}
