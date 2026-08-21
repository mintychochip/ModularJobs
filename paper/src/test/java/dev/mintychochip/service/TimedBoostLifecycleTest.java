package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.boost.RuledBoostSourceImpl;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.GlobalTarget;
import dev.mintychochip.repository.TimedBoostRepository;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Timed boost apply → applicable while valid → expired rows cleaned from storage.
 */
class TimedBoostLifecycleTest {

  private InMemoryTimedBoostRepository repository;
  private TimedBoostDataServiceImpl service;
  private BoostSource source;

  @BeforeEach
  void setUp() {
    repository = new InMemoryTimedBoostRepository();
    service = new TimedBoostDataServiceImpl(repository);
    source = new RuledBoostSourceImpl(
        List.of(new Rule((Condition) ctx -> true, 1,
            new MultiplicativeBoostImpl(new BigDecimal("2.0")))),
        Key.key("modularjobs", "timed_test"),
        "timed test source"
    );
  }

  @Test
  void applyThenFindApplicableWhileValid() {
    GlobalTarget target = new GlobalTarget();
    service.addData(new ConsumableBoostData(source, Duration.ofHours(1)), target);

    List<ActiveBoostData> applicable = service.findApplicableBoosts(target);
    assertEquals(1, applicable.size());
    assertEquals(source.key().toString(), applicable.get(0).sourceIdentifier());
    assertFalse(applicable.get(0).isExpired());
  }

  @Test
  void expiredBoostIsNotApplicableAndRemovedFromStorage() {
    final GlobalTarget target = new GlobalTarget();
    // Insert already-expired row via repository (simulates clock advancing past duration)
    Instant started = Instant.now().minus(Duration.ofHours(2));
    ActiveBoostData expired = new ActiveBoostData(
        "global",
        source.key().toString(),
        started,
        Duration.ofMinutes(5),
        source
    );
    repository.addBoost(expired);
    assertEquals(1, repository.findAllBoosts("global").size());
    assertTrue(expired.isExpired());

    List<ActiveBoostData> applicable = service.findApplicableBoosts(target);
    assertTrue(applicable.isEmpty(), "expired boost must not be applicable");
    assertTrue(repository.findAllBoosts("global").isEmpty(),
        "expired boost must be cleaned from storage");
  }

  @Test
  void permanentBoostNeverExpires() {
    GlobalTarget target = new GlobalTarget();
    ActiveBoostData permanent = new ActiveBoostData(
        "global",
        source.key().toString(),
        Instant.now().minus(Duration.ofDays(365)),
        null,
        source
    );
    repository.addBoost(permanent);

    List<ActiveBoostData> applicable = service.findApplicableBoosts(target);
    assertEquals(1, applicable.size());
    assertFalse(applicable.get(0).isExpired());
    assertEquals(1, repository.findAllBoosts("global").size());
  }

  /** Lightweight in-memory store behind the real TimedBoostDataService API. */
  static final class InMemoryTimedBoostRepository implements TimedBoostRepository {

    private final Map<String, Map<String, ActiveBoostData>> byTarget = new ConcurrentHashMap<>();

    @Override
    public @NotNull List<ActiveBoostData> findAllBoosts(String targetIdentifier) {
      Map<String, ActiveBoostData> map = byTarget.get(targetIdentifier);
      if (map == null) {
        return new ArrayList<>();
      }
      return new ArrayList<>(map.values());
    }

    @Override
    public ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier) {
      Map<String, ActiveBoostData> map = byTarget.get(targetIdentifier);
      return map == null ? null : map.get(sourceIdentifier);
    }

    @Override
    public void delete(String targetIdentifier, String sourceIdentifier) {
      Map<String, ActiveBoostData> map = byTarget.get(targetIdentifier);
      if (map != null) {
        map.remove(sourceIdentifier);
      }
    }

    @Override
    public void addBoost(ActiveBoostData boost) {
      byTarget
          .computeIfAbsent(boost.targetIdentifier(), k -> new ConcurrentHashMap<>())
          .put(boost.sourceIdentifier(), boost);
    }
  }
}
