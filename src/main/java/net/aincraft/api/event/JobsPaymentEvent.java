package net.aincraft.api.event;

import net.aincraft.api.container.Payable;
import org.bukkit.OfflinePlayer;

public class JobsPaymentEvent extends AbstractEvent {

  private final OfflinePlayer player;
  private final Payable payable;

  public JobsPaymentEvent(OfflinePlayer player, Payable payable) {
    this.player = player;
    this.payable = payable;
  }

  public OfflinePlayer getPlayer() {
    return player;
  }

  public Payable getPayable() {
    return payable;
  }
}
