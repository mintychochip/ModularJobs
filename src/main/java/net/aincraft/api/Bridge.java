package net.aincraft.api;

import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.config.ConfigurationFactory;
import org.bukkit.Bukkit;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  ConfigurationFactory configurationFactory();

  RegistryContainer registryContainer();

}
