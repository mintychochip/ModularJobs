package net.aincraft.api;

import net.aincraft.Jobs;
import net.aincraft.api.container.Provider;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.service.ChunkExplorationStore;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.JobTaskProvider;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  Jobs plugin();

  KeyResolver resolver();

  ProgressionService progressionService();

  RegistryContainer registryContainer();

  EntityValidationService spawnerService();

  JobTaskProvider jobTaskProvider();

  EconomyProvider economy();

  ExploitService exploitService();

  Provider<Block, OfflinePlayer> blockOwnershipProvider();

  MobDamageTracker mobDamageTracker();

  ChunkExplorationStore chunkExplorationStore();
}
