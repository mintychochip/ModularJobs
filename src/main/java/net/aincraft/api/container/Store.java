package net.aincraft.api.container;

public interface Store<K, V> {

  static <K, V> Store<K, V> memory() {
    return new MemoryStoreImpl<>();
  }

  V get(K key);

  boolean contains(K key);

  void remove(K key);

  void add(K key, V value) throws IllegalArgumentException;
}
