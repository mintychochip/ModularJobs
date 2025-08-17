package net.aincraft.serialization;

import java.util.List;
import net.aincraft.registry.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class CodecLoader {

  private final List<Codec> codecs = List.of(
      new CombinedBoostDataCodecImpl(),
      new PassiveBoostDataCodecImpl(),
      new ConsumableBoostDataCodecImpl(),
      new AllApplicablePolicyCodecImpl(),
      new GetFirstPolicyCodecImpl(),
      new TopKPolicyCodecImpl(),
      new ComposableConditionCodecImpl(),
      new SneakConditionCodecImpl(),
      new SprintConditionCodecImpl(),
      new WorldConditionCodecImpl(),
      new NegatingConditionCodecImpl(),
      new BiomeConditionCodecImpl(),
      new PlayerResourceConditionCodecImpl(),
      new PotionTypeConditionCodecImpl(),
      new PotionConditionCodecImpl(),
      new LiquidConditionCodecImpl(),
      new WeatherConditionCodecImpl(),
      new AdditiveBoostCodecImpl(),
      new MultiplicativeBoostCodecImpl(),
      new RuleCodecImpl(),
      new DurationCodecImpl(),
      new IntArrayCodecImpl(),
      new RuledBoostCodecImpl()
  );

  private CodecLoader() {}

  public static final CodecLoader INSTANCE = new CodecLoader();

  public void load(Registry<Codec> registry) {
    codecs.forEach(registry::register);
  }
}
