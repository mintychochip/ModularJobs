package net.aincraft.payable;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import net.aincraft.container.PayableType;
import net.aincraft.registry.Registry;
import net.aincraft.registry.SimpleRegistryImpl;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public final class PayableModule extends AbstractModule {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";

  private final Plugin plugin;

  public PayableModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    install(new PreferenceModule());
    install(new PaymentTypesModule(plugin));

    Multibinder<PayableType> types = Multibinder.newSetBinder(binder(), PayableType.class);
    types.addBinding().to(Key.get(PayableType.class, Names.named(ECONOMY_TYPE)));
    types.addBinding().to(Key.get(PayableType.class, Names.named(EXPERIENCE_TYPE)));
  }

  @Provides
  @Singleton
  Registry<PayableType> payableTypeRegistry(Set<PayableType> types) {
    Registry<PayableType> registry = new SimpleRegistryImpl<>();
    types.forEach(registry::register);
    return registry;
  }
}
