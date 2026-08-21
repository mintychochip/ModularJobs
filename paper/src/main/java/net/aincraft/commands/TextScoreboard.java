package net.aincraft.commands;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.gui.craftux.CraftuxSurfaces;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Ephemeral sidebar scoreboard backed by craftux {@link CraftuxSurfaces}.
 *
 * <p>Hosts accumulate lines then call {@link #setCurrent(Player)} to mount a
 * craftux scoreboard plan for that audience.
 */
public final class TextScoreboard {

  private static final int MAX_LINES = 15;
  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  private final CraftuxSurfaces surfaces;
  private final String title;
  private final String[] lines = new String[MAX_LINES];

  private TextScoreboard(CraftuxSurfaces surfaces, String title) {
    this.surfaces = surfaces;
    this.title = title;
  }

  public static TextScoreboard create(CraftuxSurfaces surfaces, Component displayName) {
    return new TextScoreboard(surfaces, PLAIN.serialize(displayName));
  }

  /**
   * Legacy factory without craftux surfaces.
   *
   * @deprecated use {@link #create(CraftuxSurfaces, Component)}
   */
  @Deprecated
  public static TextScoreboard create(Component displayName) {
    throw new UnsupportedOperationException(
        "TextScoreboard requires CraftuxSurfaces; use create(surfaces, title)");
  }

  public void setLine(int index, ComponentLike prefix, ComponentLike suffix) {
    if (index < 0 || index >= MAX_LINES) {
      throw new IndexOutOfBoundsException("scoreboard line " + index);
    }
    String left = prefix == null ? "" : PLAIN.serialize(prefix.asComponent());
    String right = suffix == null ? "" : PLAIN.serialize(suffix.asComponent());
    lines[index] = left + right;
  }

  public void show(Player player, Duration duration) {
    setCurrent(player);
  }

  public void setCurrent(Player player) {
    if (player == null) {
      return;
    }
    List<String> body = new ArrayList<>(MAX_LINES);
    for (String line : lines) {
      if (line != null) {
        body.add(line);
      }
    }
    surfaces.showScoreboard(player.getUniqueId(), title, body);
  }
}
