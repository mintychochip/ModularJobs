package net.aincraft;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;
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

/**
 * Immutable bridge assembled by the composition root for public plugin integrations.
 */
record BridgeImpl(
    RegistryContainer registryContainer,
    JobService jobService,
    ProfessionService professionService,
    RecipeService recipeService,
    BuffService buffService,
    StationService stationService,
    NodeHarvestService nodeHarvestService,
    @Nullable EconomyProvider economyProvider,
    ConditionFactory conditionFactory,
    BoostFactory boostFactory,
    TimedBoostDataService timedBoostDataService,
    EventBus eventBus) implements Bridge {

  /** Returns the configured economy provider, when an economy integration is available. */
  @Override
  public Optional<EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
  }
}
