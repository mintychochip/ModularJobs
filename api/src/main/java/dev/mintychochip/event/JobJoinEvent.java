package dev.mintychochip.event;

import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.Job;

/**
 * Event fired when a player joins or rejoins a job.
 */
public final class JobJoinEvent {

  private final UUID playerId;
  private final Job job;
  private final int level;
  private final boolean rejoin;

  public JobJoinEvent(UUID playerId, Job job, int level, boolean rejoin) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.job = job;
    this.level = level;
    this.rejoin = rejoin;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Job getJob() {
    return job;
  }

  /**
   * Gets the player's level (1 for new joins, restored level for rejoins).
   */
  public int getLevel() {
    return level;
  }

  /**
   * Whether this is a rejoin (player previously left this job).
   */
  public boolean isRejoin() {
    return rejoin;
  }
}
