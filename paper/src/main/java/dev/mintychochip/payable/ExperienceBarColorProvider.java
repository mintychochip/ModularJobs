package dev.mintychochip.payable;

import dev.mintychochip.preferences.api.Preference;
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

  private final @Nullable Preference<Color> preference;

  ExperienceBarColorProvider(@Nullable Preference<Color> preference) {
    this.preference = preference;
  }

  Color getColor(Player player) {
    if (preference == null) {
      return Color.GREEN;
    }
    @Nullable Color value = preference.get(player);
    return value != null ? value : Color.GREEN;
  }
}
