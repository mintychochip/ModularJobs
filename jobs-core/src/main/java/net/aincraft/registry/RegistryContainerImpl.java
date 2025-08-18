package net.aincraft.registry;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

final class RegistryContainerImpl implements RegistryContainer {

  private final Map<Key, Registry<?>> registrar = new HashMap<>();

  @Override
  public <T> boolean hasRegistry(RegistryKey<T> key) {
    return registrar.containsKey(key.key());
  }

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull <T> RegistryView<T> getRegistry(RegistryKey<T> key)
      throws IllegalArgumentException {
    Preconditions.checkArgument(hasRegistry(key));
    return (RegistryView<T>) registrar.get(key.key());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Keyed> void editRegistry(RegistryKey<T> key,
      Consumer<Registry<T>> registryConsumer) throws IllegalArgumentException {
    Preconditions.checkArgument(hasRegistry(key));
    Registry<T> registry = (Registry<T>) registrar.get(key.key());
    registryConsumer.accept(registry);
  }

  <T extends Keyed> void addRegistry(Key key, Registry<T> registry) {
    registrar.put(key.key(), registry);
  }
}
