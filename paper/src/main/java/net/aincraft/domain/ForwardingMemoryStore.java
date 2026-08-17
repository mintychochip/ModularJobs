package net.aincraft.domain;

import com.google.common.collect.ForwardingMap;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.domain.repository.MemoryStore;
import org.jetbrains.annotations.NotNull;

/**
 * Base map implementation that forwards operations to a replaceable in-memory store.
 *
 * @param <K> key type
 * @param <V> value type
 */
abstract class ForwardingMemoryStore<K, V> extends ForwardingMap<K, V> implements MemoryStore<K, V> {

  /** The backing map used by forwarded operations; replaced when the store reloads. */
  protected volatile Map<K, V> store = new HashMap<>();

  @Override
  protected @NotNull Map<K, V> delegate() {
    return store;
  }
}
