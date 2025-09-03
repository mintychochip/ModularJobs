package net.aincraft.commands;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;

public interface TextScoreboard {

  void setLine(int index, ComponentLike prefix, ComponentLike suffix);

  void show(Player player, Duration duration);

  void setCurrent(Player player);
}
