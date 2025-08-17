package net.aincraft.serialization;

import net.aincraft.boost.policy.GetFirstPolicyImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record GetFirstPolicyCodecImpl() implements Codec.Typed<GetFirstPolicyImpl> {

  @Override
  public Class<?> type() {
    return GetFirstPolicyImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:get_first_policy");
  }

  @Override
  public void encode(BinaryOut out, GetFirstPolicyImpl object, Writer writer) {

  }

  @Override
  public GetFirstPolicyImpl decode(BinaryIn in, Reader reader) {
    return GetFirstPolicyImpl.INSTANCE;
  }
}
