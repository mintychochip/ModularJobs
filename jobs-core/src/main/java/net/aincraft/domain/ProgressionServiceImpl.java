package net.aincraft.domain;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

final class ProgressionServiceImpl implements ProgressionService {

  private final JobProgressionRepository live;

  private final JobProgressionRepository archive;

  @Inject
  public ProgressionServiceImpl(
      @Named(ProgressionServiceModule.LIVE_REPOSITORY) JobProgressionRepository live,
      @Named(ProgressionServiceModule.ARCHIVE_REPOSITORY) JobProgressionRepository archive) {
    this.live = live;
    this.archive = archive;
  }

  @Override
  public boolean save(JobProgressionRecord record) {
    return live.save(record);
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
    return live.load(playerId, jobKey);
  }

  @Override
  public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
    return live.loadAllForJob(jobKey, limit);
  }

  @Override
  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
    return live.loadAllForPlayer(playerId, limit);
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    return live.delete(playerId, jobKey);
  }

  @Override
  public boolean archive(String playerId, String jobKey) {
    return migrate(live, archive, playerId, jobKey);
  }

  @Override
  public boolean restore(String playerId, String jobKey) {
    return migrate(archive, live, playerId, jobKey);
  }

  private boolean migrate(JobProgressionRepository from, JobProgressionRepository to,
      String playerId, String jobKey) {
    JobProgressionRecord record = from.load(playerId, jobKey);
    Bukkit.broadcastMessage("attempted to migrate" + jobKey);
    if (record == null) {
      return false;
    }
    if (to.save(record)) {
      Bukkit.broadcastMessage("saved");
      return from.delete(playerId, jobKey);
    }
    return false;
  }
}
