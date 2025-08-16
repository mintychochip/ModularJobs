package net.aincraft.container;

import java.math.BigDecimal;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;

public class JobProgressionImpl implements JobProgression {

  private static final int INVALID_LEVEL = -1;

  private final Job job;
  private final OfflinePlayer player;
  private final BigDecimal experience;
  private final int level;

  public JobProgressionImpl(OfflinePlayer player, Job job, BigDecimal experience, int level) {
    this.job = job;
    this.player = player;
    this.experience = experience;
    this.level = level;
  }

  public JobProgressionImpl(OfflinePlayer player, Job job, BigDecimal experience) {
    this(player, job, experience, INVALID_LEVEL);
  }

  @Override
  public JobProgression setExperience(BigDecimal experience) {
    return new JobProgressionImpl(player, job, experience, calculateCurrentLevel());
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
