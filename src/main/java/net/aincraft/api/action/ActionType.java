package net.aincraft.api.action;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

/**
 * Represents a type of in-game action, identified by a unique {@link Key}.
 *
 * <p>Implementations should override {@link Object#equals(Object)} and
 * {@link Object#hashCode()} based on their key to ensure correct identity behavior.
 */
@NonExtendable
public interface ActionType extends Keyed {

}
