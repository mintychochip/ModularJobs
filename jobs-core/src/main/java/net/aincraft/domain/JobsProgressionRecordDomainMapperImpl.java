package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.util.DomainMapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class JobsProgressionRecordDomainMapperImpl implements
    DomainMapper<JobProgression, JobProgressionRecord> {

  private final DomainMapper<Job,JobRecord> jobMapper;
  @Inject
  JobsProgressionRecordDomainMapperImpl(
      DomainMapper<Job, JobRecord> jobMapper) {
    this.jobMapper = jobMapper;
  }


  @Override
  public @NotNull JobProgression toDomainObject(@NotNull JobProgressionRecord record)
      throws IllegalArgumentException {
    Job job = jobMapper.toDomainObject(record.jobRecord());
    UUID playerId = UUID.fromString(record.playerId());
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
    return new JobProgressionImpl(player, job, record.experience());
  }
}
