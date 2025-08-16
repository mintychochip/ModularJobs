package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record WorldConditionImpl(Key worldKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return worldKey.equals(context.world().getKey());
  }

  static final class CodecImpl implements Codec.Typed<WorldConditionImpl> {

    @Override
    public Class<WorldConditionImpl> type() {
      return WorldConditionImpl.class;
    }

    @Override
    public void encode(Out out, WorldConditionImpl condition, Writer writer) {
      out.writeKey(condition.worldKey);
    }

    @Override
    public WorldConditionImpl decode(In in, Reader reader) {
      return new WorldConditionImpl(in.readKey());
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:world_condition");
    }
  }
}
