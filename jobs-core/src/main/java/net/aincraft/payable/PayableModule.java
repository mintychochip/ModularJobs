package net.aincraft.payable;

import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Set;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.registry.Registry;
import net.aincraft.registry.RegistryFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.Nullable;

public final class PayableModule extends AbstractModule {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";

  @Override
  protected void configure() {
    install(new EconomyModule());
    install(new ExperienceModule());
    Multibinder<PayableType> set = Multibinder.newSetBinder(binder(), PayableType.class);
    set.addBinding().to(Key.get(PayableType.class, Names.named(ECONOMY_TYPE)));
    set.addBinding().to(Key.get(PayableType.class, Names.named(EXPERIENCE_TYPE)));
  }

  @Provides
  @Singleton
  Registry<PayableType> payableTypeRegistry(Set<PayableType> types,
      RegistryFactory registryFactory) {
    Registry<PayableType> registry = registryFactory.simple();
    types.forEach(registry::register);
    return registry;
  }

  private static final class EconomyModule extends PrivateModule {

    @Provides
    @Singleton
    @Nullable
    public EconomyProvider economyProvider() {
      PluginManager pluginManager = Bukkit.getPluginManager();
      ServicesManager servicesManager = Bukkit.getServicesManager();
      Plugin vault = pluginManager.getPlugin("Vault");
      if (vault != null && vault.isEnabled()) {
        RegisteredServiceProvider<Economy> registration = servicesManager.getRegistration(
            Economy.class);
        if (registration != null) {
          Economy provider = registration.getProvider();
          return new VaultEconomyProviderImpl(provider);
        }
      }
      return null;
    }

    @Override
    protected void configure() {
      bind(PayableHandler.class).to(EconomyPayableHandlerImpl.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    @Exposed
    @Named(ECONOMY_TYPE)
    PayableType economy(PayableHandler handler) {
      return new EconomyPayableTypeImpl(handler, NamespacedKey.fromString(ECONOMY_TYPE));
    }
  }

  private static final class ExperienceModule extends PrivateModule {

    @Override
    protected void configure() {
      bind(ExperienceBarController.class).to(ExperienceBarControllerImpl.class).in(Singleton.class);
      bind(ExperienceBarFormatter.class).to(ExperienceBarFormatterImpl.class).in(Singleton.class);
      bind(PayableHandler.class).to(BufferedExperienceHandlerImpl.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    @Exposed
    @Named(EXPERIENCE_TYPE)
    PayableType experience(PayableHandler handler) {
      return new ExperiencePayableTypeImpl(handler, NamespacedKey.fromString(EXPERIENCE_TYPE));
    }
  }
}
