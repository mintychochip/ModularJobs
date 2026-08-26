package dev.mintychochip.payable;

import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * ModularJobs-local lookup for a player's chosen experience bar color. Returns {@code null} when
 * unset so the provider can fall back to green without touching the external Preferences API.
 */
@NullMarked
@FunctionalInterface
public interface ExperienceBarColorPreference {

  /** Looks up the player's chosen color, or {@code null} if none is set. */
  @Nullable Color color(Player player);
}
