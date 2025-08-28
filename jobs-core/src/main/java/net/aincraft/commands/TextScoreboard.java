package net.aincraft.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface TextScoreboard {
  void setLine(int index, Component line);
  void setCurrent(Player player);
}
