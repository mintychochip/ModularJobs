package net.aincraft.config;

import net.kyori.adventure.text.format.TextColor;

/**
 * Configurable color scheme for all command outputs.
 */
public record ColorScheme(
    TextColor primary,
    TextColor secondary,
    TextColor accent,
    TextColor tertiary,
    TextColor neutral,
    TextColor error
) {

  /**
   * Default color scheme with reasonable defaults.
   */
  public static ColorScheme defaults() {
    return new ColorScheme(
        TextColor.fromHexString("#FFD700"), // Gold - primary
        TextColor.fromHexString("#FFFF00"), // Yellow - secondary
        TextColor.fromHexString("#00FFFF"), // Aqua - accent
        TextColor.fromHexString("#FF00FF"), // Light purple - tertiary
        TextColor.fromHexString("#808080"), // Gray - neutral
        TextColor.fromHexString("#FF0000")  // Red - error
    );
  }
}
