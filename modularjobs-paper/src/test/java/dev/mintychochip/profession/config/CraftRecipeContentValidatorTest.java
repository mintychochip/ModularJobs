package dev.mintychochip.profession.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.action.ActionTypeImpl;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.profession.content.CraftTaskSnapshot;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class CraftRecipeContentValidatorTest {

  private static final ActionType CRAFT =
      new ActionTypeImpl("Craft", Key.key("modularjobs", "craft"));
  private static final ActionType SMELT =
      new ActionTypeImpl("Smelt", Key.key("modularjobs", "smelt"));

  @Test
  void collectCraftTasksFlattensAllJobsAndFiltersCraftAction() {
    Job blacksmith = new StubJob(Key.key("modularjobs", "blacksmith"));
    Job miner = new StubJob(Key.key("modularjobs", "miner"));
    JobTask craftTask =
        new JobTask(
            Key.key("modularjobs", "blacksmith"),
            Key.key("modularjobs", "craft"),
            Key.key("minecraft", "iron_sword"),
            List.of());
    JobTask smeltTask =
        new JobTask(
            Key.key("modularjobs", "blacksmith"),
            Key.key("modularjobs", "smelt"),
            Key.key("minecraft", "iron_ingot"),
            List.of());

    JobService jobService =
        new StubJobService(
            List.of(blacksmith, miner),
            Map.of(
                blacksmith,
                Map.of(CRAFT, List.of(craftTask), SMELT, List.of(smeltTask)),
                miner,
                Map.of()));

    List<CraftTaskSnapshot> snapshots = CraftRecipeContentValidator.collectCraftTasks(jobService);

    assertEquals(1, snapshots.size());
    assertEquals(Key.key("modularjobs", "blacksmith"), snapshots.get(0).jobKey());
    assertEquals(Key.key("minecraft", "iron_sword"), snapshots.get(0).contextKey());
    assertEquals(Key.key("minecraft", "iron_sword"), snapshots.get(0).outputKey());
  }

  @Test
  void summaryLineReportsCounts() {
    Key output = Key.key("minecraft", "stone_bricks");
    var report =
        dev.mintychochip.profession.content.CraftRecipeContentValidation.validate(
            List.of(new CraftTaskSnapshot(Key.key("modularjobs", "artisan"), output, output)),
            List.of());

    String summary = CraftRecipeContentValidator.summaryLine(report);
    assertTrue(summary.contains("1 craft task(s) without recipe metadata"));
    assertTrue(summary.contains("0 recipe(s) without craft task(s)"));
  }

  private record StubJob(Key key) implements Job {
    @Override
    public Component displayName() {
      return Component.text(key.value());
    }

    @Override
    public String getPlainName() {
      return key.value();
    }

    @Override
    public Component description() {
      return Component.text(key.value());
    }

    @Override
    public LevelingCurve levelingCurve() {
      return level -> BigDecimal.ONE;
    }

    @Override
    public Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }

    @Override
    public int maxLevel() {
      return 100;
    }

    @Override
    public int upgradeLevel() {
      return 0;
    }

    @Override
    public Map<Integer, List<String>> perkUnlocks() {
      return Map.of();
    }
  }

  private static final class StubJobService implements JobService {
    private final List<Job> jobs;
    private final Map<Job, Map<ActionType, List<JobTask>>> tasksByJob;

    private StubJobService(List<Job> jobs, Map<Job, Map<ActionType, List<JobTask>>> tasksByJob) {
      this.jobs = List.copyOf(jobs);
      this.tasksByJob = Map.copyOf(tasksByJob);
    }

    @Override
    public List<Job> getJobs() {
      return jobs;
    }

    @Override
    public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
      return tasksByJob.getOrDefault(job, Map.of());
    }

    @Override
    public Job getJob(String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public JobTask getTask(Job job, ActionType type, Context context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean update(JobProgression progression) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean joinJob(String playerId, String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean leaveJob(String playerId, String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public JobProgression getProgression(String playerId, String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<JobProgression> getProgressions(UUID playerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<JobProgression> getProgressions(Key jobKey, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<JobProgression> getArchivedProgressions(UUID playerId) {
      throw new UnsupportedOperationException();
    }
  }
}
