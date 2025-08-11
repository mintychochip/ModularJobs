package net.aincraft.api.event;

import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.container.Payable;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;

public class JobsPrePaymentEvent extends AbstractEvent implements Cancellable {

  private final OfflinePlayer player;
  private final Payable payable;
  private final Job job;
  private final JobTask jobTask;
  private boolean cancelled = false;

  public JobsPrePaymentEvent(OfflinePlayer player, Payable payable, Job job, JobTask jobTask) {
    this.player = player;
    this.payable = payable;
    this.job = job;
    this.jobTask = jobTask;
  }

  public OfflinePlayer getPlayer() {
    return player;
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
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
