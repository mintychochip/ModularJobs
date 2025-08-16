package net.aincraft.boost.conditions;

import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record SprintConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSprinting() == state;
  }

  static final class CodecImpl implements Codec.Typed<SprintConditionImpl> {

    @Override
    public Class<SprintConditionImpl> type() {
      return SprintConditionImpl.class;
    }

    @Override
    public void encode(Out out, SprintConditionImpl condition, Writer writer) {
      out.writeBool(condition.state);
    }

    @Override
    public SprintConditionImpl decode(In in, Reader reader) {
      return new SprintConditionImpl(in.readBool());
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:sprint_condition");
    }
  }
}
