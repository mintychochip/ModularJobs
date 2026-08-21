package dev.mintychochip.event;

import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;

/**
 * Fired when a player's job level changes.
 */
public final class JobLevelEvent {

  public enum Reason {
    /** Natural level up from gaining experience */
    EXPERIENCE,
    /** Admin command set/add/subtract */
    ADMIN_COMMAND,
    /** Other reasons */
    OTHER
  }

  private final UUID playerId;
  private final Job job;
  private final int oldLevel;
  private final int newLevel;
  private final JobProgression progression;
  private final Reason reason;

  public JobLevelEvent(
      UUID playerId, Job job, int oldLevel, int newLevel, JobProgression progression) {
    this(playerId, job, oldLevel, newLevel, progression, Reason.EXPERIENCE);
  }

  public JobLevelEvent(
      UUID playerId,
      Job job,
      int oldLevel,
      int newLevel,
      JobProgression progression,
      Reason reason) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.job = job;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
    this.progression = progression;
    this.reason = reason == null ? Reason.OTHER : reason;
  }

  public UUID getPlayerId() {
    return playerId;
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
