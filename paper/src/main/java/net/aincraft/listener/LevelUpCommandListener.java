package net.aincraft.listener;

import net.aincraft.paper.event.BukkitJobLevelEvent;
import net.aincraft.service.LevelUpCommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Fires configured level-up commands on job level-up.
 */
public final class LevelUpCommandListener implements Listener {

  private final LevelUpCommandExecutor executor;

  public LevelUpCommandListener(LevelUpCommandExecutor executor) {
    this.executor = executor;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLevelUp(BukkitJobLevelEvent event) {
    executor.execute(
        event.getPlayer().getName(),
        event.getJob().getPlainName(),
        event.getNewLevel());
  }
}
