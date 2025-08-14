package net.aincraft.api.container;

import net.aincraft.api.Job;
import net.kyori.adventure.bossbar.BossBar;
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
