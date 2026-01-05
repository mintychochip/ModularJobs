package net.aincraft.event;

import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class JobExperienceGainEvent extends AbstractEvent implements Cancellable {

  private final Player player;
  private final Job job;
  private final JobProgression progression;
  private BigDecimal experienceGained;
  private boolean cancelled = false;

  public JobExperienceGainEvent(Player player, Job job, JobProgression progression, BigDecimal experienceGained) {
    this.player = player;
    this.job = job;
    this.progression = progression;
    this.experienceGained = experienceGained;
  }

  public Player getPlayer() {
    return player;
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
