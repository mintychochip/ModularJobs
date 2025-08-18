package net.aincraft.registry;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.container.ActionType;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.serialization.CodecRegistry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public final class RegistryModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RegistryFactory.class).to(RegistryFactoryImpl.class).in(Singleton.class);
    MapBinder<Key, Registry<?>> registries = MapBinder.newMapBinder(binder(),
        new TypeLiteral<>() {
        }, new TypeLiteral<>() {
        });
    registries.addBinding(RegistryKeys.ACTION_TYPES.key())
        .toProvider(ActionTypeRegistryProvider.class);
    registries.addBinding(RegistryKeys.PAYABLE_TYPES.key())
        .to(com.google.inject.Key.get(new TypeLiteral<Registry<PayableType>>() {
        }));
    registries.addBinding(RegistryKeys.CODEC.key()).to(CodecRegistry.class);
  }

  @Provides
  @Singleton
  Registry<PayableType> payableTypeRegistry(Map<Key, Provider<PayableHandler>> handlers) {
    SimpleRegistryImpl<PayableType> r = new SimpleRegistryImpl<>();
    handlers.forEach((k, v) -> {
      r.register(new PayableType(v.get(), k));
    });
    return r;
  }

  @Provides
  @Singleton
  public RegistryContainer registryContainer(Map<Key, Provider<Registry<?>>> registries) {
    RegistryContainerImpl r = new RegistryContainerImpl();
    registries.forEach((k, v) -> {
      r.addRegistry(k, v.get());
    });
    return r;
  }
}
