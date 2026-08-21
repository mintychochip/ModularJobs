package dev.mintychochip;

import java.util.Optional;
import dev.mintychochip.container.EconomyProvider;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.event.EventBus;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.service.BuffService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.NodeHarvestService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.service.StationService;

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
