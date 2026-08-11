package net.aincraft;

import java.util.Optional;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.event.EventBus;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.service.BuffService;
import net.aincraft.service.JobService;
import net.aincraft.service.NodeHarvestService;
import net.aincraft.service.ProfessionService;
import net.aincraft.service.RecipeService;
import net.aincraft.service.StationService;

public interface Bridge {

  static Bridge bridge() {
    Bridge b = Holder.INSTANCE;
    if (b == null) {
      throw new IllegalStateException("Bridge not registered (plugin not enabled)");
    }
    return b;
  }

  /** Paper-only registration — called from ModularJobsBootstrap. */
  static void register(Bridge bridge) {
    Holder.INSTANCE = bridge;
  }

  static void unregister() {
    Holder.INSTANCE = null;
  }

  RegistryContainer registryContainer();

  ConditionFactory conditionFactory();

  BoostFactory boostFactory();

  TimedBoostDataService timedBoostDataService();

  Optional<EconomyProvider> economy();

  JobService jobService();

  /** Profession catalog and progression facade. */
  ProfessionService professionService();

  /** Learned profession recipes. */
  RecipeService recipeService();

  /** Consumable combat buff slots and effects. */
  BuffService buffService();

  /** Station tier gate for crafting and gathering. */
  StationService stationService();

  /** World-gather integration hook. */
  NodeHarvestService nodeHarvestService();

  /** Shared pure domain event bus. */
  EventBus eventBus();

  /** Volatile holder; nested types on interfaces are public. */
  final class Holder {
    private static volatile Bridge INSTANCE;

    private Holder() {}
  }
}
