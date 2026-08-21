package dev.mintychochip.listener;

import dev.mintychochip.paper.event.BukkitJobLevelEvent;
import dev.mintychochip.service.LevelUpCommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Fires configured level-up commands on job level-up. */
public final class LevelUpCommandListener implements Listener {

  private final LevelUpCommandExecutor executor;

  /** Level up command listener. */
  public LevelUpCommandListener(LevelUpCommandExecutor executor) {
    this.executor = executor;
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLevelUp(BukkitJobLevelEvent event) {
    executor.execute(
        event.getPlayer().getName(), event.getJob().getPlainName(), event.getNewLevel());
  }
}
