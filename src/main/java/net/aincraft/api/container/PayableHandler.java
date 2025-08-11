package net.aincraft.api.container;

import net.aincraft.api.Job;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  interface PayableContext {
    Player getPlayer();
    Payable getPayable();
    Job getJob();
  }
}
