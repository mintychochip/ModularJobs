package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.service.JobService;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Proves {@link ProfessionWiring#create} loads the bundled {@code recipes.yml} without duplicate
 * craft-output conflicts (startup-fatal with default resources).
 */
class ProfessionWiringStartupTest {

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void createLoadsBundledRecipesYmlThroughSaveResourcePath() {
    JavaPlugin plugin = MockBukkit.loadSimple(RecipeLoaderTestPlugin.class);
    ProfessionWiring wiring = ProfessionWiring.create(plugin, unusedJobService());

    assertNotNull(wiring.recipeService);
    assertTrue(
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "iron_sword"))
            .isPresent());
    assertTrue(
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "netherite_pickaxe"))
            .isPresent());
    assertEquals(
        10,
        wiring
            .recipeService
            .definitionForCraftOutput(Key.key("minecraft", "iron_pickaxe"))
            .orElseThrow()
            .requiredLevel());
  }

  private static JobService unusedJobService() {
    return new JobService() {
      @Override
      public List<Job> getJobs() {
        return List.of();
      }

      @Override
      public Job getJob(String jobKey) {
        throw new IllegalArgumentException("unknown job: " + jobKey);
      }

      @Override
      public JobTask getTask(Job job, ActionType type, Context context) {
        return null;
      }

      @Override
      public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
        return Map.of();
      }

      @Override
      public boolean update(JobProgression progression) {
        return false;
      }

      @Override
      public boolean joinJob(String playerId, String jobKey) {
        return false;
      }

      @Override
      public boolean leaveJob(String playerId, String jobKey) {
        return false;
      }

      @Override
      public JobProgression getProgression(String playerId, String jobKey) {
        throw new IllegalArgumentException("unknown job: " + jobKey);
      }

      @Override
      public List<JobProgression> getProgressions(UUID playerId) {
        return Collections.emptyList();
      }

      @Override
      public List<JobProgression> getProgressions(Key jobKey, int limit) {
        return Collections.emptyList();
      }

      @Override
      public List<JobProgression> getArchivedProgressions(UUID playerId) {
        return Collections.emptyList();
      }
    };
  }
}
