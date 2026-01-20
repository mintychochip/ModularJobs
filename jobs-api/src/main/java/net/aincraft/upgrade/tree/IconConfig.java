package net.aincraft.upgrade.tree;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for an item icon displayed in the skill tree GUI.
 * Supports both vanilla materials and custom item models.
 *
 * @param id        the Material ID (e.g., "diamond_pickaxe")
 * @param itemModel optional item model namespace:key (e.g., "modularjobs:mining/efficiency_1")
 */
public record IconConfig(
    @NotNull String id,
    @Nullable String itemModel
) {
  /**
   * Convert this config to a Bukkit Material.
   * Returns BARRIER as fallback if material is invalid.
   */
  public Material toMaterial() {
    try {
      return Material.valueOf(id.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Material.BARRIER;
    }
  }
}
