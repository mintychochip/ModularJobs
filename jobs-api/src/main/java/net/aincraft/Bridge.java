package net.aincraft;

import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.BuffService;
import net.aincraft.service.JobService;
import net.aincraft.service.NodeHarvestService;
import net.aincraft.service.ProfessionService;
import net.aincraft.service.RecipeService;
import net.aincraft.service.StationService;
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

  /** AzothMC P6 profession facade (catalog + progression). */
  ProfessionService professionService();

  /** Learned recipes (P6). */
  RecipeService recipeService();

  /** Consumable combat buff slots (P6); formulas in azoth. */
  BuffService buffService();

  /** Station tier gate (stub until territory P8). */
  StationService stationService();

  /** Gather hook (stub until world P11). */
  NodeHarvestService nodeHarvestService();
}
