package net.aincraft.api.registry;

import java.util.function.Consumer;
import net.aincraft.api.Bridge;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

public interface RegistryContainer {

  static RegistryContainer registryContainer() {
    return Bridge.bridge().registryContainer();
  }

  <T> boolean hasRegistry(RegistryKey<T> key);

  @NotNull
  <T> RegistryView<T> getRegistry(RegistryKey<T> key) throws IllegalArgumentException;

  <T extends Keyed> void editRegistry(RegistryKey<T> key, Consumer<Registry<T>> registryConsumer) throws IllegalArgumentException;
}
