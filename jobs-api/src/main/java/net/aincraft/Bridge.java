package net.aincraft;

import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.UpgradeService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public interface Bridge {

  static Bridge bridge() {
    return Bukkit.getServicesManager().load(Bridge.class);
  }

  Plugin plugin();

  RegistryContainer registryContainer();

  ConditionFactory conditionFactory();

  BoostFactory boostFactory();

  TimedBoostDataService timedBoostDataService();

  Optional<EconomyProvider> economy();

  JobService jobService();

  UpgradeService upgradeService();
}
