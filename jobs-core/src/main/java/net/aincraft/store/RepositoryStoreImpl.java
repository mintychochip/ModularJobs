package net.aincraft.store;

import net.aincraft.container.Store;
import net.aincraft.database.Repository;

public class RepositoryStoreImpl<K, V> implements Store<K, V> {

  private final Repository<K, V> repository;

  public RepositoryStoreImpl(Repository<K, V> repository) {
    this.repository = repository;
  }

  @Override
  public V get(K key) {
    return repository.load(key);
  }

  @Override
  public boolean contains(K key) {
    V load = repository.load(key);
    return load != null;
  }

  @Override
  public void remove(K key) {
    repository.delete(key);
  }

  @Override
  public void add(K key, V value) throws IllegalArgumentException {
    repository.save(key, value);
  }
}
