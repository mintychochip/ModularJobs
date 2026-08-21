package dev.mintychochip.profession;

import dev.mintychochip.profession.config.YamlRecipeDefinitionLoader;
import dev.mintychochip.service.BuffService;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.NodeHarvestService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.service.StationService;
import org.bukkit.plugin.java.JavaPlugin;

/** Manual composition for the profession service surfaces. */
public final class ProfessionWiring {

  public final ProfessionService professionService;
  public final RecipeService recipeService;
  public final BuffService buffService;
  public final StationService stationService;
  public final NodeHarvestService nodeHarvestService;

  private ProfessionWiring(
      ProfessionService professionService,
      RecipeService recipeService,
      BuffService buffService,
      StationService stationService,
      NodeHarvestService nodeHarvestService) {
    this.professionService = professionService;
    this.recipeService = recipeService;
    this.buffService = buffService;
    this.stationService = stationService;
    this.nodeHarvestService = nodeHarvestService;
  }

  /** Create. */
  public static ProfessionWiring create(JavaPlugin plugin, JobService jobService) {
    MemoryRecipeService recipeService = new MemoryRecipeService();
    YamlRecipeDefinitionLoader.load(plugin, recipeService);
    return new ProfessionWiring(
        new ProfessionServiceImpl(jobService),
        recipeService,
        new MemoryBuffService(),
        new StubStationService(),
        new StubNodeHarvestService());
  }
}
