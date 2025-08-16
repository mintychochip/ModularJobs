package net.aincraft.serialization.data;

import java.util.Collection;
import java.util.List;
import net.aincraft.container.Codec;
import net.aincraft.serialization.CodecLoader;

public final class ItemBoostDataCodecLoaderImpl implements CodecLoader {

  private final List<Codec> CODECS = List.of(
      new CombinedBoostDataImpl.CodecImpl(),
      new ConsumableBoostDataImpl.CodecImpl(),
      new PassiveBoostDataImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new ItemBoostDataCodecLoaderImpl();

  private ItemBoostDataCodecLoaderImpl() {
  }

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
