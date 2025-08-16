package net.aincraft.boost;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Codec;
import net.aincraft.api.registry.Registry;

public final class BoostCodecLoaderImpl implements CodecLoader {

  private static final List<Codec> CODECS = List.of(
      new AdditiveBoostImpl.CodecImpl(),
      new MultiplicativeBoostImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new BoostCodecLoaderImpl();

  private BoostCodecLoaderImpl() {

  }

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
