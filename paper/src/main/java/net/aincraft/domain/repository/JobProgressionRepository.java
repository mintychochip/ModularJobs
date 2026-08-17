package net.aincraft.domain.repository;

import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import org.jetbrains.annotations.Nullable;

/**
 * Repository contract for persisting and querying {@link JobProgressionRecord}s by
 * player-and-job identity. Implementations determine storage and extraction
 * semantics, including ordering of batch loads and the meaning of the {@code limit} cap.
 */
public interface JobProgressionRepository {

  /**
   * Persists a progression record, replacing any existing record with the same key.
   * @param record the record to store
   * @return {@code true} if the record was persisted
   */
  boolean save(JobProgressionRecord record);

  /**
   * Loads the progression record for the given player and job.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return the matching record, or {@code null} if absent
   */
  @Nullable
  JobProgressionRecord load(String playerId, String jobKey);

  /**
   * Loads up to {@code limit} progression records for the given job.
   * @param jobKey the job key to match
   * @param limit the maximum number of records to return
   * @return the matching records
   */
  List<JobProgressionRecord> loadAllForJob(String jobKey, int limit);

  /**
   * Loads up to {@code limit} progression records for the given player.
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching records
   */
  List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit);

  /**
   * Deletes the progression record for the given player and job.
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if a record was deleted
   */
  boolean delete(String playerId, String jobKey);

  /** Composite identity of a progression record. */
  record Key(String playerId, String jobKey) {
  }
}
