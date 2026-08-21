package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.math.ExpressionCurves;
import dev.mintychochip.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link JobResolver} plain-name / namespaced resolve and fuzzy suggest.
 */
class JobResolverImplTest {

  private Job miner;
  private Job fisherman;
  private Job lumberjack;
  private JobResolver resolver;

  @BeforeEach
  void setUp() {
    miner = job("modularjobs", "miner", "Miner");
    fisherman = job("modularjobs", "fisherman", "Fisherman");
    lumberjack = job("other", "lumberjack", "Lumberjack");
    Map<String, Job> byKey = new HashMap<>();
    byKey.put(miner.key().asString(), miner);
    byKey.put(fisherman.key().asString(), fisherman);
    byKey.put(lumberjack.key().asString(), lumberjack);
    resolver = new JobResolver(new FakeJobService(List.of(miner, fisherman, lumberjack), byKey));
  }

  @Test
  void resolvePlainNameCaseInsensitive() {
    Job found = resolver.resolve("miner");
    assertNotNull(found);
    assertEquals(miner.key(), found.key());

    Job upper = resolver.resolve("FISHERMAN");
    assertNotNull(upper);
    assertEquals(fisherman.key(), upper.key());
  }

  @Test
  void resolveFullNamespacedKey() {
    Job found = resolver.resolve("modularjobs:miner");
    assertNotNull(found);
    assertEquals("Miner", found.getPlainName());
  }

  @Test
  void resolveUnknownReturnsNull() {
    assertNull(resolver.resolve("blacksmith"));
    assertNull(resolver.resolve("modularjobs:missing"));
  }

  @Test
  void resolveInNamespacePrefersNamespacedThenPlain() {
    Job byNs = resolver.resolveInNamespace("miner", "modularjobs");
    assertNotNull(byNs);
    assertEquals(miner.key(), byNs.key());

    Job otherNs = resolver.resolveInNamespace("lumberjack", "other");
    assertNotNull(otherNs);
    assertEquals(lumberjack.key(), otherNs.key());

    assertNull(resolver.resolveInNamespace("miner", "other"));
  }

  @Test
  void suggestSimilarPrefersPrefixMatches() {
    List<String> suggestions = resolver.suggestSimilar("min", 5);
    assertFalseEmpty(suggestions);
    assertEquals("Miner", suggestions.get(0), "prefix match should rank first: " + suggestions);
  }

  @Test
  void suggestSimilarLimitsResults() {
    List<String> suggestions = resolver.suggestSimilar("m", 1);
    assertEquals(1, suggestions.size());
  }

  @Test
  void getPlainNamesListsAllJobs() {
    List<String> names = resolver.getPlainNames();
    assertEquals(3, names.size());
    assertTrue(names.contains("Miner"));
    assertTrue(names.contains("Fisherman"));
    assertTrue(names.contains("Lumberjack"));
  }

  private static void assertFalseEmpty(List<String> suggestions) {
    assertNotNull(suggestions);
    assertTrue(!suggestions.isEmpty(), "expected non-empty suggestions");
  }

  private static Job job(String namespace, String value, String displayName) {
    return new JobImpl(
        Key.key(namespace, value),
        Component.text(displayName),
        Component.text(displayName + " job"),
        50,
        ExpressionCurves.levelingCurve("level * 100"),
        Map.of(),
        30,
        Map.of()
    );
  }

  /**
   * Collaborator fake — SUT is JobResolver.
   */
  private static final class FakeJobService implements JobService {

    private final List<Job> jobs;
    private final Map<String, Job> byKey;

    FakeJobService(List<Job> jobs, Map<String, Job> byKey) {
      this.jobs = jobs;
      this.byKey = byKey;
    }

    @Override
    public @NotNull List<Job> getJobs() {
      return jobs;
    }

    @Override
    public Job getJob(String jobKey) {
      Job job = byKey.get(jobKey);
      if (job == null) {
        throw new IllegalArgumentException("unknown job: " + jobKey);
      }
      return job;
    }

    @Override
    public JobTask getTask(Job job, ActionType type, Context context) {
      throw new UnsupportedOperationException();
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
      throw new UnsupportedOperationException();
    }

    @Override
    public List<JobProgression> getProgressions(UUID playerId) {
      return List.of();
    }

    @Override
    public List<JobProgression> getProgressions(Key jobKey, int limit) {
      return List.of();
    }

    @Override
    public List<JobProgression> getArchivedProgressions(UUID playerId) {
      return List.of();
    }
  }
}
