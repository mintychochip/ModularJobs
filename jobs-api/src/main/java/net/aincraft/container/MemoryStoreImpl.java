package net.aincraft.container;

import java.util.HashMap;
import java.util.Map;

public final class MemoryStoreImpl<K,V> implements Store<K,V> {

  private final Map<K,V> store = new HashMap<>();

  @Override
  public V get(K key) {
    return store.get(key);
  }

  @Override
  public boolean contains(K key) {
    return store.containsKey(key);
  }

  @Override
  public void remove(K key) {
    store.remove(key);
  }

  @Override
  public void add(K key, V value) throws IllegalArgumentException {
    store.put(key,value);
  }

  @Override
  public String toString() {
    return store.toString();
  }
}
