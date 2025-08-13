package net.aincraft.api.container;

import net.aincraft.api.Job;
import org.bukkit.OfflinePlayer;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  interface PayableContext {
    OfflinePlayer getPlayer();
    Payable getPayable();
    Job getJob();
  }
}
