package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.job.JobProgressionImpl;
import net.aincraft.service.JobService;
import net.aincraft.util.Mapper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class JobsProgressionRecordMapperImpl implements
    Mapper<JobProgression, JobProgressionRecord> {

  private final JobService jobService;

  @Inject
  public JobsProgressionRecordMapperImpl(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public @NotNull JobProgression toDomainObject(@NotNull JobProgressionRecord record)
      throws IllegalArgumentException {
    Optional<Job> job = jobService.getJob(NamespacedKey.fromString(record.jobKey()));
    if (job.isEmpty()) {
      throw new IllegalArgumentException("failed to get job");
    }
    UUID playerId = UUID.fromString(record.playerId());
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
    return new JobProgressionImpl(player, job.get(), record.experience());
  }
}
