package dev.mintychochip.event;

import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.container.Payable;

/**
 * Fired before a jobs payment is processed.
 */
public final class JobsPrePaymentEvent implements Cancellable {

  private final UUID playerId;
  private final Payable payable;
  private final Job job;
  private final JobTask jobTask;
  private boolean cancelled;

  public JobsPrePaymentEvent(UUID playerId, Payable payable, Job job, JobTask jobTask) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.payable = payable;
    this.job = job;
    this.jobTask = jobTask;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Payable getPayable() {
    return payable;
  }

  public Job getJob() {
    return job;
  }

  public JobTask getJobTask() {
    return jobTask;
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
