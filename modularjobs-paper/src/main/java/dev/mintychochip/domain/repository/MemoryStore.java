package dev.mintychochip.domain.repository;

import java.util.Map;

/**
 * A {@link Map} whose contents can be reloaded from an underlying source, replacing the in-memory
 * state in place.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface MemoryStore<K, V> extends Map<K, V> {

  /**
   * Reloads this store from its backing source, replacing the current contents. Existing iterators
   * and holding references to individual entries may become stale after invocation.
   */
  void reload();
}
