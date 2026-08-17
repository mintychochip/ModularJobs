package net.aincraft.registry;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/**
 * A mutable registry of {@link Keyed} objects identified by their Adventure keys.
 *
 * <p>Registry identity is established by the {@link RegistryKey} used to look the
 * registry up from a {@link RegistryContainer}; a registry holds at most one object
 * per key and registration of an already-present key replaces the prior entry.</p>
 */
public interface Registry<T extends Keyed> extends RegistryView<T> {

  /**
   * Registers the given object, keyed by its Adventure key.
   *
   * <p>Registering an object whose key is already present replaces the existing
   * entry.</p>
   *
   * @param object the object to register; must not be {@code null}
   */
  void register(@NotNull T object);

}
