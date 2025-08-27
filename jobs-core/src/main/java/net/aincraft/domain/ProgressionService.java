package net.aincraft.domain;

import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import org.jetbrains.annotations.Nullable;

public interface ProgressionService {

  boolean save(JobProgressionRecord record);

  @Nullable
  JobProgressionRecord load(String playerId, String jobKey);

  List<JobProgressionRecord> loadAllForJob(String jobKey, int limit);

  List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit);

  boolean delete(String playerId, String jobKey);

  boolean archive(String playerId, String jobKey);

  boolean restore(String playerId, String jobKey);
}
