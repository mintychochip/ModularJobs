package net.aincraft;

import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.KeyResolver;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.JobTaskProvider;
import net.aincraft.service.MobDamageTracker;
import org.bukkit.plugin.Plugin;
import net.aincraft.service.BlockOwnershipService;
import net.aincraft.service.ChunkExplorationStore;
import net.aincraft.service.EntityValidationService;
import net.aincraft.service.ExploitService;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  Plugin plugin();

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
