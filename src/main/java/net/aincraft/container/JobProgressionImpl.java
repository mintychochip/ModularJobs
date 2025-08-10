package net.aincraft.container;

import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import org.bukkit.OfflinePlayer;

public class JobProgressionImpl implements JobProgression {

  private final Job job;
  private final OfflinePlayer player;
  private long experience;

  public JobProgressionImpl(Job job, OfflinePlayer player, long experience) {
    this.job = job;
    this.player = player;
    this.experience = experience;
  }

  @Override
  public void setExperience(long experience) {
    this.experience = experience;
  }

  @Override
  public Job getJob() {
    return job;
  }

  @Override
  public OfflinePlayer getPlayer() {
    return player;
  }

  @Override
  public long getExperience() {
    return experience;
  }
}
