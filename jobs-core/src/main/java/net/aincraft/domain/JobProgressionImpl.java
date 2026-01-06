package net.aincraft.domain;

import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.LevelingCurve.Parameters;
import net.aincraft.JobProgression;
import org.bukkit.OfflinePlayer;

final class JobProgressionImpl implements JobProgression {

  private static final int INVALID_LEVEL = -1;

  private final Job job;
  private final OfflinePlayer player;
  private final BigDecimal experience;
  private final int level;

  JobProgressionImpl(OfflinePlayer player, Job job, BigDecimal experience) {
    this.player = player;
    this.job = job;
    this.experience = experience;
    this.level = calculateCurrentLevel();
  }

  @Override
  public JobProgression setExperience(BigDecimal experience) {
    if (this.experience.equals(experience)) {
      return this;
    }
    return new JobProgressionImpl(player, job, experience);
  }

  @Override
  public BigDecimal experienceForLevel(int level) {
    return job.levelingCurve().evaluate(new Parameters(level));
  }

  @Override
  public Job job() {
    return job;
  }

  @Override
  public OfflinePlayer player() {
    return player;
  }

  @Override
  public BigDecimal experience() {
    return experience;
  }

  @Override
  public int level() {
    return level;
  }

  private int calculateCurrentLevel() throws IllegalStateException {
    int maxLevel = job.maxLevel();
    if (maxLevel <= 0) {
      return 1;
    }

    int low = 1;
    int level = 1;
    while (low <= maxLevel) {
      int mid = (low + maxLevel) >>> 1;
      BigDecimal requiredXpForLevel = job.levelingCurve().evaluate(new Parameters(mid));
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
        ", level=" + level() +
        "]";
  }
}
