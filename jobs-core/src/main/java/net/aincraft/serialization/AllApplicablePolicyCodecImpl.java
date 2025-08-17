package net.aincraft.serialization;

import net.aincraft.boost.policy.AllApplicablePolicyImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record AllApplicablePolicyCodecImpl() implements Codec.Typed<AllApplicablePolicyImpl> {

  @Override
  public Class<?> type() {
    return AllApplicablePolicyImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:all_applicable_policy");
  }

  @Override
  public void encode(BinaryOut out, AllApplicablePolicyImpl object, Writer writer) {

  }

  @Override
  public AllApplicablePolicyImpl decode(BinaryIn in, Reader reader) {
    return AllApplicablePolicyImpl.INSTANCE;
  }
}
