package net.aincraft.registry;

import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public interface RegistryView<T> extends Iterable<T> {
  @NotNull
  T getOrThrow(Key key) throws IllegalArgumentException;

  boolean isRegistered(Key key);

  Stream<T> stream();
}
