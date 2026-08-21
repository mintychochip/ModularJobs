package net.aincraft.domain;

import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinates job progression persistence between a primary "live" store and an
 * archival store, adding {@linkplain #archive(String, String) archive} and
 * {@linkplain #restore(String, String) restore} migration on top of the live repository.
 */
public final class ProgressionService {

  /** Primary store used by routine read/write operations. */
  private final JobProgressionRepository live;

  /** Secondary store holding archived progression records. */
  private final JobProgressionRepository archive;

  /**
   * Wires live and archive progression repositories for routine operations and migration.
   *
   * @param live the live repository backing normal progression operations
   * @param archive the repository used to hold archived progression records
   */
  public ProgressionService(
      JobProgressionRepository live,
      JobProgressionRepository archive) {
    this.live = live;
    this.archive = archive;
  }

  /**
   * Saves a progression record to the live store.
   * @param record the record to persist
   * @return {@code true} if the record was stored
   */
  public boolean save(JobProgressionRecord record) {
    return live.save(record);
  }

  /**
   * Loads a progression record from the live store.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return the matching record, or {@code null} if absent
   */
  public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
    return live.load(playerId, jobKey);
  }

  /**
   * Loads up to {@code limit} live progression records for a job.
   * @param jobKey the job key to match
   * @param limit the maximum number of records to return
   * @return the matching live records
   */
  public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
    return live.loadAllForJob(jobKey, limit);
  }

  /**
   * Loads up to {@code limit} live progression records for a player.
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching live records
   */
  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
    return live.loadAllForPlayer(playerId, limit);
  }

  /**
   * Deletes a progression record from the live store.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if a record was deleted
   */
  public boolean delete(String playerId, String jobKey) {
    return live.delete(playerId, jobKey);
  }

  /**
   * Moves a progression record from the live store to the archive.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was migrated, {@code false} if it was absent or the copy failed
   */
  public boolean archive(String playerId, String jobKey) {
    return migrate(live, archive, playerId, jobKey);
  }

  /**
   * Moves an archived progression record back to the live store.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was restored, {@code false} if it was absent or the copy failed
   */
  public boolean restore(String playerId, String jobKey) {
    return migrate(archive, live, playerId, jobKey);
  }

  /**
   * Loads up to {@code limit} archived progression records for a player.
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching archived records
   */
  public List<JobProgressionRecord> loadAllArchivedForPlayer(String playerId, int limit) {
    return archive.loadAllForPlayer(playerId, limit);
  }

  /**
   * Migrates a single progression record between two repositories: loads it from {@code from},
   * saves a copy to {@code to}, and only deletes the source once the copy succeeds.
   * @param from the source repository
   * @param to the destination repository
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was migrated, {@code false} otherwise
   */
  private boolean migrate(JobProgressionRepository from, JobProgressionRepository to,
      String playerId, String jobKey) {
    JobProgressionRecord record = from.load(playerId, jobKey);
    if (record == null) {
      return false;
    }
    if (to.save(record)) {
      return from.delete(playerId, jobKey);
    }
    return false;
  }
}
