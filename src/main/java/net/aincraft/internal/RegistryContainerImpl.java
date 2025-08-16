package net.aincraft.internal;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.aincraft.api.registry.Registry;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKey;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.service.CodecRegistryImpl;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

final class RegistryContainerImpl implements RegistryContainer {

  private final Map<Key, Registry<?>> registrar;

  private RegistryContainerImpl(Map<Key, Registry<?>> registrar) {
    this.registrar = registrar;
  }

  static RegistryContainer create() {
    Map<Key, net.aincraft.api.registry.Registry<?>> registries = new HashMap<>();
    registries.put(RegistryKeys.JOBS.key(), Registry.simple());
    registries.put(RegistryKeys.PAYABLE_TYPES.key(), Registry.simple());
    registries.put(RegistryKeys.ACTION_TYPES.key(), Registry.simple());
    registries.put(RegistryKeys.TRANSIENT_BOOST_SOURCES.key(),
        Registry.simple());
    registries.put(RegistryKeys.CODEC.key(), new CodecRegistryImpl());
    return new RegistryContainerImpl(registries);
  }

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
}
