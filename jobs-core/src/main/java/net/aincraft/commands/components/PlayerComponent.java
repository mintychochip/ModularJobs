package net.aincraft.commands.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlayerComponent implements ComponentLike {

  private final String playerId;
  private final String playerName;

  public PlayerComponent(String playerId, String playerName) {
    this.playerId = playerId;
    this.playerName = playerName;
  }

  public static PlayerComponent of(OfflinePlayer player) {
    String playerName = player.getName();
    return new PlayerComponent(player.getUniqueId().toString(),
        playerName == null ? "N/A" : playerName);
  }

  @Override
  public @NotNull Component asComponent() {
    return Component.text(playerName).hoverEvent(HoverEvent.showText(Component.text(playerId)));
  }
}
