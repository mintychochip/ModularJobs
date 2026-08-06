package net.aincraft.domain;

import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public final class ProgressionService {

  private final JobProgressionRepository live;

  private final JobProgressionRepository archive;

  public ProgressionService(
      JobProgressionRepository live,
      JobProgressionRepository archive) {
    this.live = live;
    this.archive = archive;
  }

  public boolean save(JobProgressionRecord record) {
    return live.save(record);
  }

  public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
    return live.load(playerId, jobKey);
  }

  public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
    return live.loadAllForJob(jobKey, limit);
  }

  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
    return live.loadAllForPlayer(playerId, limit);
  }

  public boolean delete(String playerId, String jobKey) {
    return live.delete(playerId, jobKey);
  }

  public boolean archive(String playerId, String jobKey) {
    return migrate(live, archive, playerId, jobKey);
  }

  public boolean restore(String playerId, String jobKey) {
    return migrate(archive, live, playerId, jobKey);
  }

  public List<JobProgressionRecord> loadAllArchivedForPlayer(String playerId, int limit) {
    return archive.loadAllForPlayer(playerId, limit);
  }

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
