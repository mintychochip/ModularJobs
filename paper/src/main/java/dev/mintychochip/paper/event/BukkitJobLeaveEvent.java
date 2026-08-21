package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.event.JobLeaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit dual-fire wrapper for {@link JobLeaveEvent}.
 */
public final class BukkitJobLeaveEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobLeaveEvent pure;

  public BukkitJobLeaveEvent(@NotNull Player player, @NotNull JobLeaveEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  public @NotNull JobLeaveEvent pure() {
    return pure;
  }

  public Job getJob() {
    return pure.getJob();
  }

  public int getFinalLevel() {
    return pure.getFinalLevel();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
