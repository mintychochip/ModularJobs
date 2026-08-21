package dev.mintychochip.registry;

import java.util.Optional;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * A read-only view over a registry of keyed objects.
 *
 * <p>Objects are addressed by their Adventure {@link Key}; iterating the view yields the registered
 * objects in an implementation-defined order.
 *
 * @param <T> the element type of the registry
 */
public interface RegistryView<T> extends Iterable<T> {
  /**
   * Returns the object registered under the given key, if any.
   *
   * @param key the object key; must not be {@code null}
   * @return an {@link Optional} containing the registered object, or empty if none is registered
   */
  @NotNull
  Optional<T> get(Key key);

  /**
   * Returns the object registered under the given key.
   *
   * @param key the object key; must not be {@code null}
   * @return the registered object
   * @throws IllegalArgumentException if no object is registered under the key
   */
  @NotNull
  T getOrThrow(Key key);

  /**
   * Tests whether an object is registered under the given key.
   *
   * @param key the object key; must not be {@code null}
   * @return {@code true} if an object is registered, {@code false} otherwise
   */
  boolean isRegistered(Key key);

  /**
   * Returns a stream over the registered objects.
   *
   * @return a stream of the registered objects
   */
  Stream<T> stream();
}
