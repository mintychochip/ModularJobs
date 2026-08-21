package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.LevelingCurve.Parameters;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobProgressionRecord;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.registry.Registry;
import java.math.BigDecimal;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

/**
 * Immutable snapshot of a player's progression within a single job.
 *
 * <p>The current {@code level} is derived from the player's {@code experience} against the job's
 * {@link LevelingCurve} at construction time. Mutations such as {@link #setExperience(BigDecimal)}
 * return a new instance; nothing is persisted here (persistence is handled by {@link
 * ProgressionService}).
 *
 * <p>Persistence-through-failure: not applicable; this is a passive value object.
 */
final class JobProgressionImpl implements JobProgression {

  private final Job job;
  private final UUID playerId;
  private final BigDecimal experience;
  private final int level;

  /**
   * Creates a progression snapshot, deriving the current level from accrued experience.
   *
   * @param playerId owning player (not validated here; must be non-null)
   * @param job job this progression tracks
   * @param experience total accrued experience
   * @throws IllegalStateException if the level cannot be derived from the curve
   */
  JobProgressionImpl(UUID playerId, Job job, BigDecimal experience) {
    this.playerId = playerId;
    this.job = job;
    this.experience = experience;
    this.level = calculateCurrentLevel();
  }

  /** Returns a new progression with the given experience; returns {@code this} if unchanged. */
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

  /**
   * Binary-searches the level whose experience threshold is met by {@link #experience}.
   *
   * @return the derived current level, clamped to {@code [1, maxLevel]}
   * @throws IllegalStateException if no level can be derived
   */
  private int calculateCurrentLevel() {
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
    return "JobProgressionImpl["
        + "player="
        + playerId
        + ", job="
        + job.key().value()
        + ", experience="
        + experience
        + ", level="
        + level()
        + "]";
  }

  /**
   * Converts this progression to its persisted record, embedding a serialized job snapshot.
   *
   * @return record for persistence
   */
  JobProgressionRecord toRecord() {
    JobRecord jobRecord = ((JobImpl) job).toRecord();
    return new JobProgressionRecord(playerId.toString(), jobRecord, experience);
  }

  /**
   * Reconstructs a progression from a persisted record.
   *
   * @param record persisted progression data
   * @param plugin plugin for key namespaces
   * @param payableTypeRegistry registry for filtering payable curves
   * @return reconstructed progression
   * @throws IllegalArgumentException if the player id, job, or curve is invalid
   */
  static JobProgressionImpl fromRecord(
      JobProgressionRecord record, Plugin plugin, Registry<PayableType> payableTypeRegistry) {
    Job job = JobImpl.fromRecord(record.jobRecord(), plugin, payableTypeRegistry);
    return new JobProgressionImpl(UUID.fromString(record.playerId()), job, record.experience());
  }
}
