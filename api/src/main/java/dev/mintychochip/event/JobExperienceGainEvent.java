package dev.mintychochip.event;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;

/**
 * Fired before experience is applied; listeners may cancel or mutate the amount.
 */
public final class JobExperienceGainEvent implements Cancellable {

  private final UUID playerId;
  private final Job job;
  private final JobProgression progression;
  private BigDecimal experienceGained;
  private boolean cancelled;

  public JobExperienceGainEvent(
      UUID playerId, Job job, JobProgression progression, BigDecimal experienceGained) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.job = job;
    this.progression = progression;
    this.experienceGained = experienceGained;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Job getJob() {
    return job;
  }

  public JobProgression getProgression() {
    return progression;
  }

  public BigDecimal getExperienceGained() {
    return experienceGained;
  }

  public void setExperienceGained(BigDecimal experienceGained) {
    this.experienceGained = experienceGained;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
