package net.aincraft.container;

import net.aincraft.Job;
import org.bukkit.OfflinePlayer;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  //TODO: add set methods

  record PayableContext(OfflinePlayer player, Payable payable, Job job) {}

  //TODO: add repair methods
  interface PayableVisualController {
    void display(PayableContext context);
  }

}
