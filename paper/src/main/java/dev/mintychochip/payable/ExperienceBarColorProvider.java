package dev.mintychochip.payable;

import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Experience bar color when preferences are not available (default green).
 */
@NullMarked
final class ExperienceBarColorProvider {

  Color getColor(Player player) {
    return Color.GREEN;
  }
}
