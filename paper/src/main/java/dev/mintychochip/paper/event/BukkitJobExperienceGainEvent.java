package dev.mintychochip.paper.event;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.event.JobExperienceGainEvent;
import java.math.BigDecimal;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit dual-fire wrapper for {@link JobExperienceGainEvent}. Cancel/amount mutate the pure event.
 */
public final class BukkitJobExperienceGainEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final JobExperienceGainEvent pure;

  /** Bukkit job experience gain event. */
  public BukkitJobExperienceGainEvent(
      @NotNull Player player, @NotNull JobExperienceGainEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull Player getPlayer() {
    return player;
  }

  /** Pure. */
  public @NotNull JobExperienceGainEvent pure() {
    return pure;
  }

  public Job getJob() {
    return pure.getJob();
  }

  public JobProgression getProgression() {
    return pure.getProgression();
  }

  public BigDecimal getExperienceGained() {
    return pure.getExperienceGained();
  }

  /** Sets the experience gained. */
  public void setExperienceGained(BigDecimal experienceGained) {
    pure.setExperienceGained(experienceGained);
  }

  @Override
  public boolean isCancelled() {
    return pure.isCancelled();
  }

  @Override
  public void setCancelled(boolean cancel) {
    pure.setCancelled(cancel);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
