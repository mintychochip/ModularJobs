package dev.mintychochip.payable;

import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the XP boss bar color from the optional per-player Preferences-backed preference. Falls
 * back to green when the preference or its service is unavailable.
 */
@NullMarked
final class ExperienceBarColorProvider {

  private final @Nullable ExperienceBarColorPreference preference;

  ExperienceBarColorProvider(@Nullable ExperienceBarColorPreference preference) {
    this.preference = preference;
  }

  Color getColor(Player player) {
    if (preference == null) {
      return Color.GREEN;
    }
    @Nullable Color value = preference.color(player);
    return value != null ? value : Color.GREEN;
  }
}
