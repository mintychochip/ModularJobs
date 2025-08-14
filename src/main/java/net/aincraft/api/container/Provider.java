package net.aincraft.api.container;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Provider<K, V> {

  @NotNull
  Optional<V> get(K key);
}
