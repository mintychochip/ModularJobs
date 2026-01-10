package net.aincraft.payable;

import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Default experience bar color provider when preferences are not available.
 */
@NullMarked
final class DefaultExperienceBarColorProvider implements ExperienceBarColorProvider {

  @Override
  public Color getColor(Player player) {
    return Color.GREEN;
  }
}
