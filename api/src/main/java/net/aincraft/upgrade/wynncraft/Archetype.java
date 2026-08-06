package net.aincraft.upgrade.wynncraft;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an archetype in a Wynncraft-style skill tree.
 * Archetypes are thematic groupings of nodes (e.g., "The Optimizer", "The Swift").
 *
 * @param id   unique identifier for this archetype
 * @param name display name shown in UI
 * @param color color theme for this archetype (used in GUI rendering)
 */
public record Archetype(
    @NotNull String id,
    @NotNull String name,
    @NotNull String color
) {
}
