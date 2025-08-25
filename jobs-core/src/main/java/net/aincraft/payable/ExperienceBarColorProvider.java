package net.aincraft.payable;

import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;

public interface ExperienceBarColorProvider {

  Color getColor(Player player);
}
