package net.aincraft.bridge;

import net.aincraft.Jobs;
import net.aincraft.api.Bridge;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.database.ConnectionSource;
import net.aincraft.economy.Economy;
import net.aincraft.economy.VaultEconomy;
import net.aincraft.service.ProgressionService;
import net.aincraft.service.ProgressionServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class BridgeImpl implements Bridge {

  private final Jobs plugin;
  private final ConnectionSource connectionSource;
  private final RegistryContainer registryContainer = new RegistryContainerImpl();
  private final ProgressionService progressionService;
  private final KeyResolver keyResolver = new KeyResolverImpl();

  public BridgeImpl(Jobs plugin, ConnectionSource connectionSource) {
    this.plugin = plugin;
    this.connectionSource = connectionSource;
    progressionService = new ProgressionServiceImpl(connectionSource);
  }

  @Override
  public Jobs plugin() {
    return plugin;
  }

  @Override
  public KeyResolver resolver() {
    return keyResolver;
  }

  @Override
  public ProgressionService progressionService() {
    return progressionService;
  }

  @Override
  public RegistryContainer registryContainer() {
    return registryContainer;
  }

  @Override
  public Economy economy() {
    Plugin vault = Bukkit.getServer().getPluginManager().getPlugin("Vault");
    if (vault != null) {
      RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration = Bukkit.getServicesManager()
          .getRegistration(net.milkbowl.vault.economy.Economy.class);
      net.milkbowl.vault.economy.Economy provider = registration.getProvider();
      return new VaultEconomy(provider);
    }
    return null;
  }

}
