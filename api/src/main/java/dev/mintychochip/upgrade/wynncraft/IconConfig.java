package dev.mintychochip.upgrade.wynncraft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for an item icon displayed in the skill tree GUI.
 * Supports both vanilla materials and custom item models.
 *
 * @param id        the material name (e.g., "diamond_pickaxe", "DIAMOND_PICKAXE")
 * @param itemModel optional item model namespace:key (e.g., "modularjobs:mining/efficiency_1")
 */
public record IconConfig(
    @NotNull String id,
    @Nullable String itemModel
) {
}
