package net.aincraft.event;

import net.aincraft.container.Payable;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;

public class JobsPaymentEvent extends AbstractEvent implements Cancellable {

  private final OfflinePlayer player;
  private final Payable base;
  private boolean cancelled = false;

  public JobsPaymentEvent(OfflinePlayer player, Payable base) {
    this.player = player;
    this.base = base;
  }

  public OfflinePlayer getPlayer() {
    return player;
  }

  public Payable getBase() {
    return base;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
