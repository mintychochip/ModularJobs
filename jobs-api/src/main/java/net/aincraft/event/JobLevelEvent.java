package net.aincraft.event;

import net.aincraft.Job;
import org.bukkit.entity.Player;

public class JobLevelEvent extends AbstractEvent {

  private final Player player;
  private final Job job;
  private final int oldLevel;
  private final int newLevel;

  public JobLevelEvent(Player player, Job job, int oldLevel, int newLevel) {
    this.player = player;
    this.job = job;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
  }

  public Player getPlayer() {
    return player;
  }

  public Job getJob() {
    return job;
  }

  public int getOldLevel() {
    return oldLevel;
  }

  public int getNewLevel() {
    return newLevel;
  }

  /**
   * @deprecated Use {@link #getNewLevel()} instead
   */
  @Deprecated
  public int getLevel() {
    return newLevel;
  }
}
