package dev.mintychochip.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class WriteBackRepositoryImpl<K, V> {

  private final RelationalRepositoryImpl<K, V> delegate;

  private final Cache<K, V> readCache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(1)).maximumSize(100).build();
  private final Map<K, V> pendingUpserts = new ConcurrentHashMap<>();
  private final Set<K> pendingDeletes = ConcurrentHashMap.newKeySet();
  private final int maxBatch = 50;
  private final ReentrantLock flushLock = new ReentrantLock();

  private final AtomicBoolean flushing = new AtomicBoolean(false);

  /** Max wait for an in-flight scheduled flush before disable flush fails loudly. */
  private static final long FLUSH_LOCK_WAIT_MS = 30_000L;

  public WriteBackRepositoryImpl(RelationalRepositoryImpl<K, V> delegate) {
    this.delegate = delegate;
  }

  public static <K, V> WriteBackRepositoryImpl<K, V> create(Plugin plugin, RelationalRepositoryImpl<K, V> delegate, long periodSeconds) {
    WriteBackRepositoryImpl<K, V> writeBehindRepository = new WriteBackRepositoryImpl<>(delegate);
    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> writeBehindRepository.flush(), 0L, periodSeconds, TimeUnit.SECONDS);
    return writeBehindRepository;
  }

  public @Nullable V load(K key) {
    if (pendingDeletes.contains(key)) {
      return null;
    }
    V staged = pendingUpserts.get(key);
    if (staged != null) {
      return staged;
    }
    return readCache.get(key, delegate::load);
  }

  public boolean save(K key, V value) {
    flushLock.lock();
    try {
      pendingDeletes.remove(key);
      pendingUpserts.put(key, value);
      readCache.put(key, value);
      return true;
    } finally {
      flushLock.unlock();
    }
  }

  public void delete(K key) {
    flushLock.lock();
    try {
      pendingUpserts.remove(key);
      pendingDeletes.add(key);
      readCache.invalidate(key);
    } finally {
      flushLock.unlock();
    }
  }

  private void flush() {
    if (!flushing.compareAndSet(false, true)) {
      return;
    }
    try {
      flushOnce();
    } finally {
      flushing.set(false);
    }
  }

  /**
   * Drain all pending upserts/deletes to the relational delegate. Called on plugin disable
   * before ConnectionSource shutdown. Waits with sleep (not busy-spin) for an in-flight
   * scheduled flush; times out instead of hanging the main thread forever.
   */
  public void flushPending() {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FLUSH_LOCK_WAIT_MS);
    while (!flushing.compareAndSet(false, true)) {
      if (System.nanoTime() >= deadline) {
        throw new IllegalStateException(
            "Timed out waiting for write-back flush lock after " + FLUSH_LOCK_WAIT_MS + "ms");
      }
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted waiting for write-back flush", e);
      }
    }
    try {
      while (flushOnce()) {
        // drain batches
      }
    } finally {
      flushing.set(false);
    }
  }

  private boolean flushOnce() {
    flushLock.lock();
    try {
      // Snapshot pending operations while blocking concurrent save/delete calls. Entries stay
      // queued until every delegate operation succeeds, so unchecked failures are lossless.
      List<K> deletes = new ArrayList<>();
      Iterator<K> keyIterator = this.pendingDeletes.iterator();
      while (keyIterator.hasNext() && deletes.size() < maxBatch) {
        deletes.add(keyIterator.next());
      }

      Map<K, V> upserts = new LinkedHashMap<>();
      Iterator<Entry<K, V>> iterator = pendingUpserts.entrySet().iterator();
      while (iterator.hasNext() && upserts.size() < maxBatch) {
        Entry<K, V> element = iterator.next();
        upserts.put(element.getKey(), element.getValue());
      }

      if (deletes.isEmpty() && upserts.isEmpty()) {
        return false;
      }

      for (K deletedKey : deletes) {
        delegate.delete(deletedKey);
      }
      if (!upserts.isEmpty()) {
        upserts.forEach(delegate::save);
      }
      deletes.forEach(pendingDeletes::remove);
      upserts.forEach((key, value) -> pendingUpserts.remove(key, value));
      return true;
    } finally {
      flushLock.unlock();
    }
  }
}
