package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.domain.model.JobProgressionRecord;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.repository.JobProgressionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link ProgressionService} archive/restore migration against in-memory repos. */
class ProgressionServiceArchiveTest {

  private InMemoryProgressionRepository live;
  private InMemoryProgressionRepository archive;
  private ProgressionService service;
  private JobRecord job;
  private JobProgressionRecord progression;

  @BeforeEach
  void setUp() {
    live = new InMemoryProgressionRepository();
    archive = new InMemoryProgressionRepository();
    service = new ProgressionService(live, archive);
    job =
        new JobRecord(
            "modularjobs:miner",
            "Miner",
            "Mines blocks",
            100,
            "level * 100",
            Map.of("currency", "base"),
            30,
            Map.of());
    progression = new JobProgressionRecord("player-1", job, new BigDecimal("1500.50"));
  }

  @Test
  void saveAndLoadFromLive() {
    assertTrue(service.save(progression));
    JobProgressionRecord loaded = service.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals(new BigDecimal("1500.50"), loaded.experience());
    assertEquals("modularjobs:miner", loaded.jobRecord().jobKey());
  }

  @Test
  void archiveMovesRecordFromLiveToArchive() {
    assertTrue(service.save(progression));
    assertTrue(service.archive("player-1", "modularjobs:miner"));

    assertNull(service.load("player-1", "modularjobs:miner"), "must leave live store");
    assertNull(live.load("player-1", "modularjobs:miner"));

    List<JobProgressionRecord> archived = service.loadAllArchivedForPlayer("player-1", 10);
    assertEquals(1, archived.size());
    assertEquals(new BigDecimal("1500.50"), archived.get(0).experience());
    assertNotNull(archive.load("player-1", "modularjobs:miner"));
  }

  @Test
  void restoreMovesRecordFromArchiveToLive() {
    assertTrue(service.save(progression));
    assertTrue(service.archive("player-1", "modularjobs:miner"));
    assertTrue(service.restore("player-1", "modularjobs:miner"));

    JobProgressionRecord restored = service.load("player-1", "modularjobs:miner");
    assertNotNull(restored);
    assertEquals(new BigDecimal("1500.50"), restored.experience());
    assertTrue(service.loadAllArchivedForPlayer("player-1", 10).isEmpty());
    assertNull(archive.load("player-1", "modularjobs:miner"));
  }

  @Test
  void archiveMissingRecordReturnsFalse() {
    assertFalse(service.archive("missing", "modularjobs:miner"));
  }

  @Test
  void restoreMissingRecordReturnsFalse() {
    assertFalse(service.restore("missing", "modularjobs:miner"));
  }

  @Test
  void deleteRemovesLiveProgression() {
    assertTrue(service.save(progression));
    assertTrue(service.delete("player-1", "modularjobs:miner"));
    assertNull(service.load("player-1", "modularjobs:miner"));
  }

  @Test
  void loadAllForPlayerRespectsLimit() {
    JobRecord fisher =
        new JobRecord(
            "modularjobs:fisherman", "Fisher", "Fish", 50, "level*10", Map.of(), 20, Map.of());
    service.save(progression);
    service.save(new JobProgressionRecord("player-1", fisher, BigDecimal.TEN));

    List<JobProgressionRecord> limited = service.loadAllForPlayer("player-1", 1);
    assertEquals(1, limited.size());

    List<JobProgressionRecord> all = service.loadAllForPlayer("player-1", 10);
    assertEquals(2, all.size());
  }

  /** In-memory fake for collaborator only — SUT is ProgressionService. */
  private static final class InMemoryProgressionRepository implements JobProgressionRepository {

    private final Map<String, JobProgressionRecord> store = new ConcurrentHashMap<>();

    private static String key(String playerId, String jobKey) {
      return playerId + "|" + jobKey;
    }

    @Override
    public boolean save(JobProgressionRecord record) {
      store.put(key(record.playerId(), record.jobRecord().jobKey()), record);
      return true;
    }

    @Override
    public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
      return store.get(key(playerId, jobKey));
    }

    @Override
    public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
      return store.values().stream()
          .filter(r -> r.jobRecord().jobKey().equals(jobKey))
          .limit(limit)
          .toList();
    }

    @Override
    public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
      List<JobProgressionRecord> result = new ArrayList<>();
      for (JobProgressionRecord record : store.values()) {
        if (record.playerId().equals(playerId)) {
          result.add(record);
          if (result.size() >= limit) {
            break;
          }
        }
      }
      return result;
    }

    @Override
    public boolean delete(String playerId, String jobKey) {
      return store.remove(key(playerId, jobKey)) != null;
    }
  }
}
