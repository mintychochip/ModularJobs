package net.aincraft.registry;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.util.Map;
import net.aincraft.container.ActionType;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Key;

public final class RegistryModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Registry<ActionType>>() {
    }).toProvider(ActionTypeRegistryProvider.class).in(Singleton.class);
    MapBinder<Key, Registry<?>> registries = MapBinder.newMapBinder(binder(),
        new TypeLiteral<>() {
        }, new TypeLiteral<>() {
        });
    registries.addBinding(RegistryKeys.ACTION_TYPES.key())
        .to(com.google.inject.Key.get(new TypeLiteral<Registry<ActionType>>() {
        }));
    registries.addBinding(RegistryKeys.PAYABLE_TYPES.key())
        .to(com.google.inject.Key.get(new TypeLiteral<Registry<PayableType>>() {
        }));
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
