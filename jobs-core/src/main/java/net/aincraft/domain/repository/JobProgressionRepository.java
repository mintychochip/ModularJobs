package net.aincraft.domain.repository;

import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import org.jetbrains.annotations.Nullable;

public interface JobProgressionRepository {

  boolean save(JobProgressionRecord record);

  @Nullable
  JobProgressionRecord load(String playerId, String jobKey);

  List<JobProgressionRecord> loadAllForJob(String jobKey, int limit);

  List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit);

  boolean delete(String playerId, String jobKey);

  record Key(String playerId, String jobKey) {
  }
}
