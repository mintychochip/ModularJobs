package dev.mintychochip.container;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

/**
 * Represents a type of in-game action a player can perform.
 */
@NonExtendable
public interface ActionType extends Keyed {

  String name();
}
