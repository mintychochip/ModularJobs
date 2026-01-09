package net.aincraft.event;

import net.aincraft.Job;
import net.aincraft.JobProgression;
import org.bukkit.entity.Player;

public class JobLevelEvent extends AbstractEvent {

  public enum Reason {
    /** Natural level up from gaining experience */
    EXPERIENCE,
    /** Admin command set/add/subtract */
    ADMIN_COMMAND,
    /** Other reasons */
    OTHER
  }

  private final Player player;
  private final Job job;
  private final int oldLevel;
  private final int newLevel;
  private final JobProgression progression;
  private final Reason reason;

  public JobLevelEvent(Player player, Job job, int oldLevel, int newLevel, JobProgression progression) {
    this(player, job, oldLevel, newLevel, progression, Reason.EXPERIENCE);
  }

  public JobLevelEvent(Player player, Job job, int oldLevel, int newLevel, JobProgression progression, Reason reason) {
    this.player = player;
    this.job = job;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
    this.progression = progression;
    this.reason = reason;
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

  public JobProgression getProgression() {
    return progression;
  }

  public Reason getReason() {
    return reason;
  }
}
