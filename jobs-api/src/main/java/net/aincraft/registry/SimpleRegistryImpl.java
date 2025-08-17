package net.aincraft.registry;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

final class SimpleRegistryImpl<T extends Keyed> implements Registry<T> {

  private final Map<Key, T> registry = new HashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  @Override
  public @NotNull T getOrThrow(Key key) throws IllegalArgumentException {
    Preconditions.checkArgument(isRegistered(key));
    readWriteLock.readLock().lock();
    try {
      return registry.get(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public boolean isRegistered(Key key) {
    readWriteLock.readLock().lock();
    try {
      return registry.containsKey(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public Stream<T> stream() {
    return registry.values().stream();
  }

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