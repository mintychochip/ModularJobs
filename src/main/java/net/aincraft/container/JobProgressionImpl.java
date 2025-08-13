package net.aincraft.container;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableTypes;
import org.bukkit.OfflinePlayer;

public class JobProgressionImpl implements JobProgression {

  private final Job job;
  private final OfflinePlayer player;
  private double experience;
  private int level = -1;

  public JobProgressionImpl(Job job, OfflinePlayer player, double experience) {
    this.job = job;
    this.player = player;
    this.experience = experience;
  }

  @Override
  public void setExperience(double experience) {
    this.experience = experience;
    this.level = getLevel();
  }

  @Override
  public double getExperienceForLevel(int level) {
    PayableCurve curve = job.getCurve(PayableTypes.EXPERIENCE);
    return curve.apply(Map.of("level",level)).doubleValue();
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
  public double getExperience() {
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
    BigDecimal currentXp = BigDecimal.valueOf(experience);

    int low = 1;
    int level = -1;
    while (low <= maxLevel) {
      int mid = (low + maxLevel) >> 1;
      BigDecimal requiredXpForLevel = job.getCurve(PayableTypes.EXPERIENCE).apply(Map.of("level",mid));
      if (currentXp.compareTo(requiredXpForLevel) >= 0) {
        level = mid;
        low = mid + 1;
      } else {
        maxLevel = mid - 1;
      }
    }
    return level;
  }
}
