package net.aincraft.upgrade.wynncraft;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for an item icon displayed in the skill tree GUI.
 * Supports both vanilla materials and custom model data for resource packs.
 *
 * @param id               the Material ID (e.g., "diamond_pickaxe")
 * @param customModelData optional custom model data integer for resource pack support
 */
public record IconConfig(
    @NotNull String id,
    int customModelData
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
