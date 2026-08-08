package net.aincraft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.domain.repository.JobProgressionRepository.Key;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link WriteBackJobProgressionRepositoryImpl}: flush re-queue must not clobber
 * newer XP; loadAllForJob pending-delete keys on job key (not player id).
 */
class WriteBackJobProgressionIntegrityTest {

  private MemoryDelegate delegate;
  private WriteBackJobProgressionRepositoryImpl writeBack;

  @BeforeEach
  void setUp() {
    delegate = new MemoryDelegate();
    writeBack = WriteBackJobProgressionRepositoryImpl.createUnscheduled(delegate, 50, 50);
  }

  @Test
  void requeueFailedBatchKeepsHigherExperienceAlreadyStaged() {
    JobRecord job = job("modularjobs:miner");
    Key key = new Key("player-1", job.jobKey());
    JobProgressionRecord older = new JobProgressionRecord("player-1", job, new BigDecimal("100"));
    JobProgressionRecord newer = new JobProgressionRecord("player-1", job, new BigDecimal("150"));

    writeBack.save(newer);
    Map<Key, JobProgressionRecord> batch = new HashMap<>();
    batch.put(key, older);
    writeBack.requeueFailedBatch(batch, Set.of());

    JobProgressionRecord loaded = writeBack.load("player-1", job.jobKey());
    assertEquals(0, new BigDecimal("150").compareTo(loaded.experience()),
        "re-queue must not putAll older XP over newer pending");
  }

  @Test
  void requeueFailedBatchRestoresWhenNothingNewer() {
    JobRecord job = job("modularjobs:miner");
    Key key = new Key("player-1", job.jobKey());
    JobProgressionRecord only = new JobProgressionRecord("player-1", job, new BigDecimal("40"));
    Map<Key, JobProgressionRecord> batch = new HashMap<>();
    batch.put(key, only);
    writeBack.requeueFailedBatch(batch, Set.of());
    assertEquals(0, new BigDecimal("40").compareTo(
        writeBack.load("player-1", job.jobKey()).experience()));
  }

  @Test
  void loadAllForJobPendingDeleteMatchesJobKeyNotPlayerId() {
    JobRecord miner = job("modularjobs:miner");
    JobRecord fisher = job("modularjobs:fisherman");
    // playerId deliberately equals a job key string to catch the old bug
    String playerId = "modularjobs:miner";
    writeBack.save(new JobProgressionRecord(playerId, miner, new BigDecimal("10")));
    writeBack.save(new JobProgressionRecord(playerId, fisher, new BigDecimal("20")));
    writeBack.delete(playerId, miner.jobKey());

    List<JobProgressionRecord> forMiner = writeBack.loadAllForJob(miner.jobKey(), 100);
    assertTrue(forMiner.isEmpty(), "pending delete for miner must remove miner rows");

    List<JobProgressionRecord> forFisher = writeBack.loadAllForJob(fisher.jobKey(), 100);
    assertEquals(1, forFisher.size());
    assertEquals(fisher.jobKey(), forFisher.getFirst().jobRecord().jobKey());
  }

  @Test
  void preferHigherExperienceChoosesMax() {
    JobRecord job = job("modularjobs:miner");
    JobProgressionRecord low = new JobProgressionRecord("p", job, new BigDecimal("5"));
    JobProgressionRecord high = new JobProgressionRecord("p", job, new BigDecimal("9"));
    assertEquals(high, writeBack.preferHigherExperience(low, high));
    assertEquals(high, writeBack.preferHigherExperience(high, low));
  }

  private static JobRecord job(String key) {
    return new JobRecord(
        key, key, "desc", 100, "x", Map.of(), 0, Map.of());
  }

  private static final class MemoryDelegate implements JobProgressionRepository {
    private final Map<String, JobProgressionRecord> store = new ConcurrentHashMap<>();

    private static String k(String playerId, String jobKey) {
      return playerId + "\0" + jobKey;
    }

    @Override
    public boolean save(JobProgressionRecord record) {
      store.put(k(record.playerId(), record.jobRecord().jobKey()), record);
      return true;
    }

    @Override
    public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
      return store.get(k(playerId, jobKey));
    }

    @Override
    public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
      return store.values().stream()
          .filter(r -> jobKey.equals(r.jobRecord().jobKey()))
          .limit(limit)
          .toList();
    }

    @Override
    public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
      return store.values().stream()
          .filter(r -> playerId.equals(r.playerId()))
          .limit(limit)
          .toList();
    }

    @Override
    public boolean delete(String playerId, String jobKey) {
      return store.remove(k(playerId, jobKey)) != null;
    }
  }
}
