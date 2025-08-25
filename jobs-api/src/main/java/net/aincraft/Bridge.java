package net.aincraft;

import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.JobTaskProvider;
import net.aincraft.service.ProgressionService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  Plugin plugin();

  ProgressionService progressionService();

  RegistryContainer registryContainer();

  JobTaskProvider jobTaskProvider();

  ConditionFactory conditionFactory();

  BoostFactory boostFactory();

  PolicyFactory policyFactory();

  TimedBoostDataService timedBoostDataService();

  Optional<EconomyProvider> economy();
}
