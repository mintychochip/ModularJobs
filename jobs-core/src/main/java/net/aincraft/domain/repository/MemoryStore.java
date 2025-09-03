package net.aincraft.domain.repository;

import java.util.Map;

public interface MemoryStore<K, V> extends Map<K, V> {

  void reload();
}
