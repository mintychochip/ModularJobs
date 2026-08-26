package dev.mintychochip;

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
import java.util.Optional;

/** Bridge. */
public interface Bridge {

  /** Bridge. */
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

  /** Unregister. */
  static void unregister() {
    Holder.INSTANCE = null;
  }

  /** Registry container. */
  RegistryContainer registryContainer();

  /** Condition factory. */
  ConditionFactory conditionFactory();

  /** Boost factory. */
  BoostFactory boostFactory();

  /** Timed boost data service. */
  TimedBoostDataService timedBoostDataService();

  /** Economy. */
  Optional<EconomyProvider> economy();

  /** Job service. */
  JobService jobService();

  /** Profession service. */
  ProfessionService professionService();

  /** Recipe service. */
  RecipeService recipeService();

  /** Buff service. */
  BuffService buffService();

  /** Station service. */
  StationService stationService();

  /** Node harvest service. */
  NodeHarvestService nodeHarvestService();

  /** Event bus. */
  EventBus eventBus();

  /** Volatile holder; nested types on interfaces are public. */
  final class Holder {
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile Bridge INSTANCE;

    private Holder() {}
  }
}
