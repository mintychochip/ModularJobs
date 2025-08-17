package net.aincraft.serialization;

import net.aincraft.boost.conditions.WorldConditionImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record WorldConditionCodecImpl() implements Codec.Typed<WorldConditionImpl> {

  @Override
  public Class<WorldConditionImpl> type() {
    return WorldConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, WorldConditionImpl condition, Writer writer) {
    out.writeKey(condition.worldKey());
  }

  @Override
  public WorldConditionImpl decode(BinaryIn in, Reader reader) {
    return new WorldConditionImpl(in.readKey());
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:world_condition");
  }
}
