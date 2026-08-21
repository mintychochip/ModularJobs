package dev.mintychochip.event;

import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.Job;

/**
 * Event fired when a player leaves a job.
 */
public final class JobLeaveEvent {

  private final UUID playerId;
  private final Job job;
  private final int finalLevel;

  public JobLeaveEvent(UUID playerId, Job job, int finalLevel) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.job = job;
    this.finalLevel = finalLevel;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Job getJob() {
    return job;
  }

  /**
   * Gets the player's level at time of leaving.
   */
  public int getFinalLevel() {
    return finalLevel;
  }
}
