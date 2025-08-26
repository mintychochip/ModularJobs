package net.aincraft.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

final class WriteBackJobProgressionRepositoryImpl implements JobProgressionRepository {

  private static Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(5);
  private static int CACHE_MAX_SIZE = 1000;

  private final JobProgressionRepository delegate;

  private record FlatKey(String playerId, String jobKey) {

  }

  private final Cache<FlatKey, JobProgressionRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(
          CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAX_SIZE).build();

  private final Map<FlatKey, JobProgressionRecord> pendingUpserts = new ConcurrentHashMap<>();
  private final Set<FlatKey> pendingDeletes = ConcurrentHashMap.newKeySet();
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
    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, __ -> {
    }, 0L, rate, rateUnit);
    return repository;
  }

  private void flush() {
    if (!flushing.compareAndSet(false, true)) {
      return;
    }
    Map<FlatKey, JobProgressionRecord> batchUpserts = new HashMap<>();
    Iterator<Entry<FlatKey, JobProgressionRecord>> iterator = pendingUpserts.entrySet().iterator();
    while (iterator.hasNext() && batchUpserts.size() < upsertBatchSize) {
      Entry<FlatKey, JobProgressionRecord> entry = iterator.next();
      FlatKey key = entry.getKey();
      JobProgressionRecord value = entry.getValue();
      if (pendingUpserts.remove(key, value)) {
        batchUpserts.put(key, value);
      }
    }
    Set<FlatKey> batchDeletes = new HashSet<>();
    Iterator<FlatKey> deleteIterator = pendingDeletes.iterator();
    while (deleteIterator.hasNext() && pendingDeletes.size() < deleteBatchSize) {
      FlatKey key = deleteIterator.next();
      if (pendingDeletes.remove(key)) {
        batchDeletes.add(key);
      }
    }
    try {
      if (!batchUpserts.isEmpty()) {
        batchUpserts.forEach((__, record) -> {
          delegate.save(record);
        });
      }
      for (FlatKey key : batchDeletes) {
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
    FlatKey key = new FlatKey(record.playerId(), record.jobRecord().jobKey());
    pendingDeletes.remove(key);
    pendingUpserts.put(key, record);
    readCache.put(key, record);
    return true;
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey)
      throws IllegalArgumentException {
    FlatKey key = new FlatKey(playerId, jobKey);
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
  public List<JobProgressionRecord> loadAll(String jobKey, int limit)
      throws IllegalArgumentException {
    List<JobProgressionRecord> records = delegate.loadAll(jobKey, limit);
    for (JobProgressionRecord record : records) {
      readCache.put();
    }
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    return false;
  }
}
