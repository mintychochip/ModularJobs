package net.aincraft.boost.conditions;

import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Codec;
import net.aincraft.boost.CodecLoader;
import net.aincraft.boost.conditions.PotionTypeConditionImpl.CodecImpl;

public final class ConditionCodecLoaderImpl implements CodecLoader {

  private static final List<Codec> CODECS = List.of(
      new ComposableConditionImpl.CodecImpl(),
      new SneakConditionImpl.CodecImpl(),
      new SprintConditionImpl.CodecImpl(),
      new WorldConditionImpl.CodecImpl(),
      new NegatingConditionImpl.CodecImpl(),
      new BiomeConditionImpl.CodecImpl(),
      new PlayerResourceConditionImpl.CodecImpl(),
      new CodecImpl(),
      new PotionConditionImpl.CodecImpl(),
      new LiquidConditionImpl.CodecImpl(),
      new WeatherConditionImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new ConditionCodecLoaderImpl();

  private ConditionCodecLoaderImpl() {
  }

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
