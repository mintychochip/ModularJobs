package net.aincraft.container;

public interface Store<K, V> {

  V get(K key);

  boolean contains(K key);

  void remove(K key);

  void add(K key, V value) throws IllegalArgumentException;
}
