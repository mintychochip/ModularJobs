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

  public JobProgressionImpl(Job job, OfflinePlayer player, double experience) {
    this.job = job;
    this.player = player;
    this.experience = experience;
  }

  @Override
  public void setExperience(double experience) {
    this.experience = experience;
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
  public int calculateLevel() throws IllegalStateException {
    int level = 0;
    int maxLevel = job.getMaxLevel();
    BigDecimal xp = BigDecimal.valueOf(experience);

    for (int i = 1; i <= maxLevel; i++) {
      Map<String,Number> variables = new HashMap<>();
      variables.put("level",i);
      PayableCurve curve = job.getCurve(PayableTypes.EXPERIENCE);
      BigDecimal requiredXp = curve.apply(variables);
      if (xp.compareTo(requiredXp) < 0) {
        break;
      }

      level = i;
    }

    return level;
  }
}
