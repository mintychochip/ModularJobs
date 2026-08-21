package dev.mintychochip.commands.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a player's name as a component whose hover text shows the player's
 * UUID, so leaderboards can display a friendly name while retaining the id.
 */
public final class PlayerComponent implements ComponentLike {

  private final String playerId;
  private final String playerName;

  /**
   * Creates a player component from explicit id and display name.
   *
   * @param playerId   player's UUID string (shown on hover)
   * @param playerName display name (rendered as the component text)
   */
  public PlayerComponent(String playerId, String playerName) {
    this.playerId = playerId;
    this.playerName = playerName;
  }

  /**
   * Builds a component from an offline player, falling back to {@code "N/A"}
   * when the player's name is unavailable.
   *
   * @param player player to render
   * @return new player component
   */
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
