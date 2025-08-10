package net.aincraft.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.aincraft.api.registry.Registry;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKey;
import net.aincraft.api.registry.RegistryView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

public final class RegistryContainerImpl implements RegistryContainer {

  private final Map<Key, Registry<?>> registrar = new HashMap<>();

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull <T> RegistryView<T> getRegistry(RegistryKey<T> key) {
    return (RegistryView<T>) registrar.computeIfAbsent(key.key(), ignored -> Registry.simple());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Keyed> void editRegistry(RegistryKey<T> key, Consumer<Registry<T>> registryConsumer) {
    Registry<T> registry = (Registry<T>) registrar.computeIfAbsent(key.key(),
        ignored -> Registry.simple());
    registryConsumer.accept(registry);
  }
}
