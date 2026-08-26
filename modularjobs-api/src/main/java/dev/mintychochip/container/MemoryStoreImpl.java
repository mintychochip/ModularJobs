package dev.mintychochip.container;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link Store} backed by a {@link HashMap}.
 *
 * <p>This implementation is neither thread-safe nor persistent: data lives only for the lifetime of
 * this instance and is lost when it is garbage collected. Like {@link HashMap}, it permits {@code
 * null} keys and values and does not support concurrent modification during iteration.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class MemoryStoreImpl<K, V> implements Store<K, V> {

  private final Map<K, V> store = new HashMap<>();

  /** {@inheritDoc} */
  @Override
  public V get(K key) {
    return store.get(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(K key) {
    return store.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public void remove(K key) {
    store.remove(key);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Adds or replaces the mapping for the key. No exception is thrown by this implementation,
   * which is backed by {@link HashMap#put(Object,Object)}.
   */
  @Override
  public void add(K key, V value) {
    store.put(key, value);
  }

  /**
   * Returns a string representation of the underlying map.
   *
   * @return the {@link HashMap#toString()} of the backing store
   */
  @Override
  public String toString() {
    return store.toString();
  }
}
