package net.aincraft.economy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.aincraft.container.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.Nullable;

public final class EconomyModule extends AbstractModule {

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
}
