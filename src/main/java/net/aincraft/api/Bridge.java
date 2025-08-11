package net.aincraft.api;

import net.aincraft.Jobs;
import net.aincraft.api.container.ExperienceBarFormatter;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.economy.Economy;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  Jobs plugin();

  KeyResolver resolver();

  ProgressionService progressionService();

  RegistryContainer registryContainer();

  ExperienceBarFormatter experienceBarFormatter();

  Economy economy();
}
