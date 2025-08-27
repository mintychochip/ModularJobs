package net.aincraft.container;

import net.aincraft.JobProgression;
import org.bukkit.OfflinePlayer;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  record PayableContext(OfflinePlayer player, Payable payable, JobProgression jobProgression) {}

  interface PayableVisualController {
    void display(PayableContext context);
  }

}
