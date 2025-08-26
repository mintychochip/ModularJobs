package net.aincraft.domain.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.aincraft.domain.model.JobProgressionRecord;
import org.jetbrains.annotations.Nullable;

public interface JobProgressionRepository {

  void save(JobProgressionRecord record);

  @Nullable
  JobProgressionRecord load(String playerId, String jobKey) throws IllegalArgumentException;

  List<JobProgressionRecord> loadAll(String jobKey, int limit) throws IllegalArgumentException;

  boolean delete(String playerId, String jobKey);
}
