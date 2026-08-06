package net.aincraft.domain;

import com.google.common.collect.ForwardingMap;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.domain.repository.MemoryStore;
import org.jetbrains.annotations.NotNull;

abstract class ForwardingMemoryStore<K, V> extends ForwardingMap<K, V> implements MemoryStore<K, V> {

  protected volatile Map<K, V> store = new HashMap<>();

  @Override
  protected @NotNull Map<K, V> delegate() {
    return store;
  }
}
