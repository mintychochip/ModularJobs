package dev.mintychochip.container;

/**
 * A key-value store abstraction.
 *
 * <p>Implementations may be persistent or ephemeral and may or may not allow {@code null} keys and
 * values; such behaviour is implementation-specific. No method in this contract guarantees
 * atomicity or thread safety.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Store<K, V> {

  /**
   * Returns the value stored for the given key.
   *
   * @param key the key to look up
   * @return the stored value, or {@code null} if absent (and where {@code null} values are not
   *     permitted)
   */
  V get(K key);

  /**
   * Returns whether a mapping exists for the given key.
   *
   * @param key the key to check
   * @return {@code true} if the key has an associated value, {@code false} otherwise
   */
  boolean contains(K key);

  /**
   * Removes the mapping for the given key, if one exists.
   *
   * @param key the key to remove
   */
  void remove(K key);

  /**
   * Adds or replaces the mapping for the given key.
   *
   * @param key the key to store under
   * @param value the value to store
   * @throws IllegalArgumentException if the key or value is rejected by the implementation, for
   *     example when unsupported values are supplied
   */
  void add(K key, V value);
}
