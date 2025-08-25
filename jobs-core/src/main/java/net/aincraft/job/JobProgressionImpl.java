package net.aincraft.job;

import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class JobProgressionImpl implements JobProgression {

  private static final int INVALID_LEVEL = -1;

  private final Job job;
  private final OfflinePlayer player;
  private final BigDecimal experience;
  private final int level;

  public JobProgressionImpl(OfflinePlayer player, Job job, BigDecimal experience) {
    this.player = player;
    this.job = job;
    this.experience = experience;
    this.level = calculateCurrentLevel();
  }

  @Override
  public JobProgression setExperience(BigDecimal experience) {
    return new JobProgressionImpl(player, job, experience);
  }

  @Override
  public BigDecimal getExperienceForLevel(int level) {
    return job.getLevelingCurve().evaluate(level);
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
  public BigDecimal getExperience() {
    return experience;
  }

  @Override
  public int getLevel() {
    return level;
  }

  private int calculateCurrentLevel() throws IllegalStateException {
    int maxLevel = job.getMaxLevel();
    if (maxLevel <= 0) {
      return 1;
    }

    int low = 1;
    int level = INVALID_LEVEL;
    while (low <= maxLevel) {
      int mid = (low + maxLevel) >>> 1;
      BigDecimal requiredXpForLevel = job.getLevelingCurve().evaluate(mid);
      if (experience.compareTo(requiredXpForLevel) >= 0) {
        level = mid;
        low = mid + 1;
      } else {
        maxLevel = mid - 1;
      }
    }
    return level;
  }

  @Override
  public String toString() {
    return "JobProgressionImpl[" +
        "player=" + player.getUniqueId() +
        ", job=" + job.key().value() +
        ", experience=" + experience +
        ", level=" + getLevel() +
        "]";
  }
}
