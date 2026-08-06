package net.aincraft.domain;

import java.math.BigDecimal;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.LevelingCurve.Parameters;
import net.aincraft.JobProgression;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.registry.Registry;
import org.bukkit.plugin.Plugin;

final class JobProgressionImpl implements JobProgression {

  private final Job job;
  private final UUID playerId;
  private final BigDecimal experience;
  private final int level;

  JobProgressionImpl(UUID playerId, Job job, BigDecimal experience) {
    this.playerId = playerId;
    this.job = job;
    this.experience = experience;
    this.level = calculateCurrentLevel();
  }

  @Override
  public JobProgression setExperience(BigDecimal experience) {
    if (this.experience.equals(experience)) {
      return this;
    }
    return new JobProgressionImpl(playerId, job, experience);
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
  public UUID playerId() {
    return playerId;
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
    int level = 1; // Start at level 1, upgrade if XP thresholds are met
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
        "player=" + playerId +
        ", job=" + job.key().value() +
        ", experience=" + experience +
        ", level=" + level() +
        "]";
  }

  public JobProgressionRecord toRecord() {
    JobRecord jobRecord = ((JobImpl) job).toRecord();
    return new JobProgressionRecord(
        playerId.toString(),
        jobRecord,
        experience
    );
  }

  public static JobProgressionImpl fromRecord(
      JobProgressionRecord record,
      Plugin plugin,
      Registry<PayableType> payableTypeRegistry
  ) {
    Job job = JobImpl.fromRecord(record.jobRecord(), plugin, payableTypeRegistry);
    return new JobProgressionImpl(
        UUID.fromString(record.playerId()),
        job,
        record.experience()
    );
  }
}
