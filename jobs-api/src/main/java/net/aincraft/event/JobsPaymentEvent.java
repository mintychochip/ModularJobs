package net.aincraft.event;

import net.aincraft.container.Payable;
import org.bukkit.OfflinePlayer;

public class JobsPaymentEvent extends AbstractEvent {

  private final OfflinePlayer player;
  private final Payable base;

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
}
