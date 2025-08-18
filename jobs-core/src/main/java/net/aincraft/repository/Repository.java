package net.aincraft.repository;

import org.jetbrains.annotations.Nullable;

public interface Repository<K, V> {

  @Nullable
  V load(K key);

  boolean save(K key, V value);

  void delete(K key);
}
