package net.aincraft.serialization;

import java.util.Collection;
import java.util.List;
import net.aincraft.container.Codec;
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
