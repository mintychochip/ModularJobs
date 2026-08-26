package dev.mintychochip.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

/**
 * Identifies a typed registry within a {@link RegistryContainer}.
 *
 * <p>Equality is delegated to the underlying Adventure {@link Key}, so two keys wrapping the same
 * key value are interchangeable regardless of their type parameter.
 *
 * @param <T> the element type of the addressed registry
 */
public sealed interface RegistryKey<T> extends Keyed permits RegistryKeyImpl {

  /**
   * Creates a registry key from the given Adventure key.
   *
   * @param key the identifying key; must not be {@code null}
   * @param <T> the element type of the addressed registry
   * @return a registry key wrapping the given key
   */
  static <T> RegistryKey<T> key(Key key) {
    return new RegistryKeyImpl<>(key);
  }
}
