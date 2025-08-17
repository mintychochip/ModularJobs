package net.aincraft.serialization;

import net.aincraft.boost.conditions.SprintConditionImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record SprintConditionCodecImpl() implements Codec.Typed<SprintConditionImpl> {

  @Override
  public Class<SprintConditionImpl> type() {
    return SprintConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, SprintConditionImpl condition, Writer writer) {
    out.writeBool(condition.state());
  }

  @Override
  public SprintConditionImpl decode(BinaryIn in, Reader reader) {
    return new SprintConditionImpl(in.readBool());
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:sprint_condition");
  }
}
