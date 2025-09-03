package net.aincraft.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import net.aincraft.registry.Registry;
import net.aincraft.registry.RegistryFactory;
import net.aincraft.registry.RegistryKeys;
import net.kyori.adventure.key.Key;

public final class SerializationModule extends AbstractModule {

  @Override
  protected void configure() {
    MapBinder<Key, Registry<?>> binder = MapBinder.newMapBinder(binder(), new TypeLiteral<>() {
        },
        new TypeLiteral<>() {
        });
    binder.addBinding(RegistryKeys.CODEC.key()).to(CodecRegistry.class);
  }

  @Provides
  @Singleton
  public CodecRegistry codecRegistry(RegistryFactory factory) {
    CodecRegistryImpl registry = new CodecRegistryImpl(factory.simple());
    registry.register(new ConsumableBoostDataCodecImpl());
    registry.register(new PassiveBoostDataCodecImpl());
    registry.register(new SlotSetCodecImpl());
    registry.register(new AllApplicablePolicyCodecImpl());
    registry.register(new GetFirstPolicyCodecImpl());
    registry.register(new TopKPolicyCodecImpl());
    registry.register(new ComposableConditionCodecImpl());
    registry.register(new SneakConditionCodecImpl());
    registry.register(new SprintConditionCodecImpl());
    registry.register(new WorldConditionCodecImpl());
    registry.register(new NegatingConditionCodecImpl());
    registry.register(new BiomeConditionCodecImpl());
    registry.register(new PlayerResourceConditionCodecImpl());
    registry.register(new PotionTypeConditionCodecImpl());
    registry.register(new PotionConditionCodecImpl());
    registry.register(new LiquidConditionCodecImpl());
    registry.register(new WeatherConditionCodecImpl());
    registry.register(new AdditiveBoostCodecImpl());
    registry.register(new MultiplicativeBoostCodecImpl());
    registry.register(new RuleCodecImpl());
    registry.register(new DurationCodecImpl());
    registry.register(new RuledBoostCodecImpl());
    return registry;
  }
}
