package net.aincraft.container;

import java.math.BigDecimal;
import java.util.Map;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableTypes;
import org.bukkit.OfflinePlayer;

public class JobProgressionImpl implements JobProgression {

  private static final int INVALID_LEVEL = -1;

  private final Job job;
  private final OfflinePlayer player;
  private final BigDecimal experience;
  private final int level;

  public JobProgressionImpl(Job job, OfflinePlayer player, BigDecimal experience, int level) {
    this.job = job;
    this.player = player;
    this.experience = experience;
    this.level = level;
  }

  public JobProgressionImpl(Job job, OfflinePlayer player, BigDecimal experience) {
    this(job, player, experience, INVALID_LEVEL);
  }

  @Override
  public JobProgression setExperience(BigDecimal experience) {
    return new JobProgressionImpl(job, player, experience, calculateLevel());
  }

  @Override
  public double getExperienceForLevel(int level) {
    PayableCurve curve = job.getCurve(PayableTypes.EXPERIENCE);
    return curve.apply(Map.of("level", level)).doubleValue();
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
    if (level < 0) {
      return calculateLevel();
    }
    return level;
  }

  private int calculateLevel() throws IllegalStateException {
    int maxLevel = job.getMaxLevel();
    if (maxLevel <= 0) {
      return 1;
    }

    int low = 1;
    int level = INVALID_LEVEL;
    while (low <= maxLevel) {
      int mid = (low + maxLevel) >> 1;
      BigDecimal requiredXpForLevel = job.getCurve(PayableTypes.EXPERIENCE)
          .apply(Map.of("level", mid));
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
