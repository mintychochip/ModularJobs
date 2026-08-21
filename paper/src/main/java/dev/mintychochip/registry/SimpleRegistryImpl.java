package dev.mintychochip.registry;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/** Thread-safe keyed registry backed by an in-memory map. */
public final class SimpleRegistryImpl<T extends Keyed> implements Registry<T> {

  private final Map<Key, T> registry = new HashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  /** Looks up an object without throwing when the key is absent. */
  @Override
  public @NotNull Optional<T> get(Key key) {
    return Optional.ofNullable(registry.get(key));
  }

  /** Looks up an object and rejects keys that are not registered. */
  @Override
  public @NotNull T getOrThrow(Key key) {
    Preconditions.checkArgument(isRegistered(key));
    readWriteLock.readLock().lock();
    try {
      return registry.get(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /** Returns whether an object is registered under the key. */
  @Override
  public boolean isRegistered(Key key) {
    readWriteLock.readLock().lock();
    try {
      return registry.containsKey(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /** Streams the currently registered objects. */
  @Override
  public Stream<T> stream() {
    return registry.values().stream();
  }

  /** Registers or replaces an object using its key. */
  @Override
  public void register(@NotNull T object) {
    readWriteLock.writeLock().lock();
    try {
      registry.put(object.key(), object);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return registry.values().iterator();
  }

  @Override
  public String toString() {
    return registry.toString();
  }
}
