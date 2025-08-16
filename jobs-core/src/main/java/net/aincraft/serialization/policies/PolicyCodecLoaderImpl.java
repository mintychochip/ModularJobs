package net.aincraft.serialization.policies;

import java.util.Collection;
import java.util.List;
import net.aincraft.container.Codec;
import net.aincraft.serialization.CodecLoader;

public class PolicyCodecLoaderImpl implements CodecLoader {

  private final List<Codec> CODECS = List.of(
      new AllApplicablePolicyImpl.CodecImpl(),
      new GetFirstPolicyImpl.CodecImpl(),
      new TopNPolicyImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new PolicyCodecLoaderImpl();

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
