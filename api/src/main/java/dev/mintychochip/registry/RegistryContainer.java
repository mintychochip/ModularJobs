package dev.mintychochip.registry;

import java.util.function.Consumer;
import dev.mintychochip.Bridge;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to the registries owned by the running plugin.
 *
 * <p>Registries are addressed by their {@link RegistryKey}; an unknown key causes
 * the lookup and mutation methods to throw {@link IllegalArgumentException}, which
 * callers may use to detect whether a registry exists.</p>
 */
public interface RegistryContainer {

  /**
   * Returns the {@code RegistryContainer} of the running plugin.
   *
   * @return the bridge's registry container
   */
  static RegistryContainer registryContainer() {
    return Bridge.bridge().registryContainer();
  }

  /**
   * Tests whether a registry is present for the given key.
   *
   * @param key the registry key; must not be {@code null}
   * @return {@code true} if a registry is registered under the key, {@code false} otherwise
   */
  <T> boolean hasRegistry(RegistryKey<T> key);

  /**
   * Returns the read-only view of the registry identified by the given key.
   *
   * @param key the registry key; must not be {@code null}
   * @return the registry view
   * @throws IllegalArgumentException if no registry exists under the key
   */
  @NotNull
  <T> RegistryView<T> getRegistry(RegistryKey<T> key) throws IllegalArgumentException;

  /**
   * Applies the given consumer to the mutable registry identified by the given key.
   *
   * <p>Mutations made by the consumer are applied to the backing registry, e.g. to
   * register new entries.</p>
   *
   * @param key the registry key; must not be {@code null}
   * @param registryConsumer the consumer applying mutations to the registry; must not be {@code null}
   * @throws IllegalArgumentException if no registry exists under the key
   */
  <T extends Keyed> void editRegistry(RegistryKey<T> key, Consumer<Registry<T>> registryConsumer) throws IllegalArgumentException;
}
