package net.aincraft.boost.conditions;

import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record SneakConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSneaking() == state;
  }

  static final class CodecImpl implements Codec.Typed<SneakConditionImpl> {

    @Override
    public Class<SneakConditionImpl> type() {
      return SneakConditionImpl.class;
    }

    @Override
    public void encode(Out out, SneakConditionImpl condition, Writer writer) {
      out.writeBool(condition.state);
    }

    @Override
    public Condition decode(In in, Reader reader) {
      return new SneakConditionImpl(in.readBool());
    }

    @Override
    public @NotNull Key key() {
      return Key.key("conditions:sneak");
    }
  }
}
