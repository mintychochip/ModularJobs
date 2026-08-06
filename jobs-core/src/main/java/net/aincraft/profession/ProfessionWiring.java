package net.aincraft.profession;

import net.aincraft.service.BuffService;
import net.aincraft.service.JobService;
import net.aincraft.service.NodeHarvestService;
import net.aincraft.service.ProfessionService;
import net.aincraft.service.RecipeService;
import net.aincraft.service.StationService;

/**
 * Manual composition for AzothMC P6 profession APIs.
 */
public final class ProfessionWiring {

  public final ProfessionService professionService;
  public final RecipeService recipeService;
  public final BuffService buffService;
  public final StationService stationService;
  public final NodeHarvestService nodeHarvestService;
  public final TierAntiFarmEngine antiFarmEngine;

  private ProfessionWiring(
      ProfessionService professionService,
      RecipeService recipeService,
      BuffService buffService,
      StationService stationService,
      NodeHarvestService nodeHarvestService,
      TierAntiFarmEngine antiFarmEngine) {
    this.professionService = professionService;
    this.recipeService = recipeService;
    this.buffService = buffService;
    this.stationService = stationService;
    this.nodeHarvestService = nodeHarvestService;
    this.antiFarmEngine = antiFarmEngine;
  }

  public static ProfessionWiring create(JobService jobService) {
    return create(jobService, TierAntiFarmConfig.defaults());
  }

  public static ProfessionWiring create(JobService jobService, TierAntiFarmConfig antiFarmConfig) {
    return new ProfessionWiring(
        new ProfessionServiceImpl(jobService),
        new MemoryRecipeService(),
        new MemoryBuffService(),
        new StubStationService(),
        new StubNodeHarvestService(),
        new TierAntiFarmEngine(antiFarmConfig)
    );
  }
}
