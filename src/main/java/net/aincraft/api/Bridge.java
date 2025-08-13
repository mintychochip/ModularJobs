package net.aincraft.api;

import net.aincraft.Jobs;
import net.aincraft.api.container.ExperienceBarFormatter;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.BlockOwnershipService;
import net.aincraft.api.service.ChunkExplorationStore;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.JobTaskProvider;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.economy.Economy;
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

  EntityValidationService spawnerService();

  JobTaskProvider jobTaskProvider();

  Economy economy();

  ExploitService exploitService();

  BlockOwnershipService blockOwnershipService();

  MobDamageTracker mobDamageTracker();

  ChunkExplorationStore chunkExplorationStore();
}
