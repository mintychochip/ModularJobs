package net.aincraft.api;

import java.util.Optional;
import net.aincraft.Jobs;
import net.aincraft.api.container.PolicyFactory;
import net.aincraft.api.container.boost.factories.BoostFactory;
import net.aincraft.api.container.boost.factories.ConditionFactory;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.service.BlockOwnershipService;
import net.aincraft.api.service.ChunkExplorationStore;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.JobTaskProvider;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.economy.EconomyProvider;
import org.bukkit.Bukkit;

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

  ConditionFactory conditionFactory();

  BoostFactory boostFactory();

  PolicyFactory policyFactory();

  ExploitService exploitService();

  MobDamageTracker mobDamageTracker();

  ChunkExplorationStore chunkExplorationStore();

  Optional<EconomyProvider> economy();

  Optional<BlockOwnershipService> blockOwnershipService();
}
