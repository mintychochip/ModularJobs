package net.aincraft.paper.event;

import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.event.JobLevelEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit dual-fire wrapper for {@link JobLevelEvent}.
 */
public final class BukkitJobLevelEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobLevelEvent pure;

  public BukkitJobLevelEvent(@NotNull Player player, @NotNull JobLevelEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  public @NotNull JobLevelEvent pure() {
    return pure;
  }

  public Job getJob() {
    return pure.getJob();
  }

  public int getOldLevel() {
    return pure.getOldLevel();
  }

  public int getNewLevel() {
    return pure.getNewLevel();
  }

  /**
   * Returns the legacy level value; use {@link #getNewLevel()} instead.
   *
   * @deprecated Use {@link #getNewLevel()} instead
   */
  @Deprecated
  public int getLevel() {
    return pure.getLevel();
  }

  public JobProgression getProgression() {
    return pure.getProgression();
  }

  public JobLevelEvent.Reason getReason() {
    return pure.getReason();
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
