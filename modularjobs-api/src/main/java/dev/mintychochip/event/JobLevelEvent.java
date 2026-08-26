package dev.mintychochip.event;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import java.util.Objects;
import java.util.UUID;

/** Fired when a player's job level changes. */
public final class JobLevelEvent {

  /** Reason. */
  public enum Reason {
    EXPERIENCE,
    ADMIN_COMMAND,
    OTHER
  }

  private final UUID playerId;
  private final Job job;
  private final int oldLevel;
  private final int newLevel;
  private final JobProgression progression;
  private final Reason reason;

  /** Job level event. */
  public JobLevelEvent(
      UUID playerId, Job job, int oldLevel, int newLevel, JobProgression progression) {
    this(playerId, job, oldLevel, newLevel, progression, Reason.EXPERIENCE);
  }

  /** Job level event. */
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
   * Returns the new level.
   *
   * @deprecated Use {@link #getNewLevel()} instead.
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
