package net.aincraft.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

final class WriteBackJobProgressionRepositoryImpl implements JobProgressionRepository {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(5);

  private static final int CACHE_MAX_SIZE = 1000;

  private final JobProgressionRepository delegate;

  private final Cache<Key, JobProgressionRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(
          CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAX_SIZE).build();

  private final Map<Key, JobProgressionRecord> pendingUpserts = new ConcurrentHashMap<>();
  private final Set<Key> pendingDeletes = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean flushing = new AtomicBoolean(false);
  private final int upsertBatchSize;
  private final int deleteBatchSize;

  private WriteBackJobProgressionRepositoryImpl(JobProgressionRepository delegate,
      int upsertBatchSize,
      int deleteBatchSize) {
    this.delegate = delegate;
    this.upsertBatchSize = upsertBatchSize;
    this.deleteBatchSize = deleteBatchSize;
  }

  public static WriteBackJobProgressionRepositoryImpl create(Plugin plugin,
      JobProgressionRepository delegate,
      int upsertBatchSize, int deleteBatchSize, long rate, TimeUnit rateUnit) {
    WriteBackJobProgressionRepositoryImpl repository = new WriteBackJobProgressionRepositoryImpl(
        delegate, upsertBatchSize, deleteBatchSize);
    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
      repository.flush();
    }, 0L, rate, rateUnit);
    return repository;
  }

  private void flush() {
    if (!flushing.compareAndSet(false, true)) {
      return;
    }
    Map<Key, JobProgressionRecord> batchUpserts = new HashMap<>();
    Iterator<Entry<Key, JobProgressionRecord>> iterator = pendingUpserts.entrySet().iterator();
    while (iterator.hasNext() && batchUpserts.size() < upsertBatchSize) {
      Entry<Key, JobProgressionRecord> entry = iterator.next();
      Key key = entry.getKey();
      JobProgressionRecord value = entry.getValue();
      if (pendingUpserts.remove(key, value)) {
        batchUpserts.put(key, value);
      }
    }
    Set<Key> batchDeletes = new HashSet<>();
    Iterator<Key> deleteIterator = pendingDeletes.iterator();
    while (deleteIterator.hasNext() && batchDeletes.size() < deleteBatchSize) {
      Key key = deleteIterator.next();
      if (pendingDeletes.remove(key)) {
        batchDeletes.add(key);
      }
    }
    try {
      batchUpserts.forEach((__, record) -> {
        delegate.save(record);
      });
      for (Key key : batchDeletes) {
        delegate.delete(key.playerId(), key.jobKey());
      }

    } catch (Throwable t) {
      pendingUpserts.putAll(batchUpserts);
      pendingDeletes.addAll(batchDeletes);
    } finally {
      flushing.set(false);
    }
  }

  @Override
  public boolean save(JobProgressionRecord record) {
    Key key = new Key(record.playerId(), record.jobRecord().jobKey());
    pendingDeletes.remove(key);
    pendingUpserts.put(key, record);
    readCache.put(key, record);
    return true;
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey)
      throws IllegalArgumentException {
    Key key = new Key(playerId, jobKey);
    if (pendingDeletes.contains(key)) {
      return null;
    }
    JobProgressionRecord record = pendingUpserts.get(key);
    if (record != null) {
      return record;
    }
    return readCache.get(key, __ -> delegate.load(playerId, jobKey));
  }

  @Override
  public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit)
      throws IllegalArgumentException {
    List<JobProgressionRecord> base = delegate.loadAllForJob(jobKey, limit);
    Map<Key, JobProgressionRecord> merged = new HashMap<>();
    for (JobProgressionRecord record : base) {
      merged.put(new Key(record.playerId(), record.jobRecord().jobKey()), record);
    }
    for (Key key : pendingDeletes) {
      if (jobKey.equals(key.playerId())) {
        merged.remove(key);
      }
    }
    for (Key key : pendingUpserts.keySet()) {
      JobProgressionRecord record = pendingUpserts.get(key);
      if (record == null) {
        continue;
      }
      merged.put(key, record);
    }
    List<JobProgressionRecord> records = new ArrayList<>(merged.values());
    records.sort(
        Comparator.comparing(JobProgressionRecord::experience,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(JobProgressionRecord::playerId)
            .thenComparing(r -> r.jobRecord().jobKey())
    );
    return records;
  }

  @Override
  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit)
      throws IllegalArgumentException {
    List<JobProgressionRecord> base = delegate.loadAllForPlayer(playerId, limit);
    Map<Key, JobProgressionRecord> merged = new HashMap<>();
    for (JobProgressionRecord record : base) {
      Key key = new Key(record.playerId(), record.jobRecord().jobKey());
      merged.put(key, record);
    }
    for (Key key : pendingDeletes) {
      if (playerId.equals(key.playerId())) {
        merged.remove(key);
      }
    }
    for (Key key : pendingUpserts.keySet()) {
      JobProgressionRecord record = pendingUpserts.get(key);
      if (record == null) {
        continue;
      }
      merged.put(key, record);
    }
    List<JobProgressionRecord> records = new ArrayList<>(merged.values());
    records.sort(
        Comparator.comparing(JobProgressionRecord::experience,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(JobProgressionRecord::playerId)
            .thenComparing(r -> r.jobRecord().jobKey())
    );
    return records;
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    Key key = new Key(playerId, jobKey);
    pendingUpserts.remove(key);
    pendingDeletes.add(key);
    readCache.invalidate(key);
    return true;
  }
}
