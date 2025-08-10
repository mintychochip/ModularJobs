package net.aincraft.api.registry;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public interface RegistryView<T> {
  @NotNull
  T getOrThrow(Key key) throws IllegalArgumentException;

  boolean isRegistered(Key key);
}
