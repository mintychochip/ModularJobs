package net.aincraft.container;

import net.aincraft.Job;
import org.bukkit.OfflinePlayer;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  //TODO: add set methods

  interface PayableContext {
    OfflinePlayer getPlayer();
    Payable getPayable();
    Job getJob();
  }

  //TODO: add repair methods
  interface PayableVisualController {
    void display(PayableContext context);
  }

}
