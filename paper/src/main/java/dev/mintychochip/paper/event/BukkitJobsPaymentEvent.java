package dev.mintychochip.paper.event;

import dev.mintychochip.container.Payable;
import dev.mintychochip.event.JobsPaymentEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit dual-fire wrapper for {@link JobsPaymentEvent}.
 */
public final class BukkitJobsPaymentEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final OfflinePlayer player;
  private final JobsPaymentEvent pure;

  public BukkitJobsPaymentEvent(@NotNull OfflinePlayer player, @NotNull JobsPaymentEvent pure) {
    this.player = player;
    this.pure = pure;
  }

  public @NotNull OfflinePlayer getPlayer() {
    return player;
  }

  public @NotNull JobsPaymentEvent pure() {
    return pure;
  }

  public Payable getBase() {
    return pure.getBase();
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
