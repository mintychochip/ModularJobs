package net.aincraft.api;

import net.aincraft.api.context.KeyResolver;
import net.kyori.adventure.key.Key;

/**
 * Represents a type of in-game action, identified by a unique {@link Key}.
 *
 * <p>Implementations should override {@link Object#equals(Object)} and
 * {@link Object#hashCode()} based on their key to ensure correct identity behavior.
 */
public interface ActionType {

  /**
   * An {@link ActionType} that can resolve a context object of type {@code C}
   * to a {@link Key}, typically for actions involving materials, items, or entities.
   *
   * @param <C> the context type this action resolves
   */
  interface Resolving<C> extends ActionType {

    /**
     * Returns the resolver used to map context values to keys.
     *
     * @return the context resolver
     */
    KeyResolver<C> resolver();
  }
}
