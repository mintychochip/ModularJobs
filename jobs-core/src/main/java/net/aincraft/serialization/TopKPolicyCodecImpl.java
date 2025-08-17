package net.aincraft.serialization;

import net.aincraft.boost.policy.TopKPolicyImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record TopKPolicyCodecImpl() implements Codec.Typed<TopKPolicyImpl> {

  @Override
  public void encode(BinaryOut out, TopKPolicyImpl object, Writer writer) {
    out.writeUnsignedInt(object.k());
  }

  @Override
  public TopKPolicyImpl decode(BinaryIn in, Reader reader) {
    return new TopKPolicyImpl(in.readUnsignedInt());
  }

  @Override
  public Class<?> type() {
    return TopKPolicyImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:top_n_policy");
  }
}
