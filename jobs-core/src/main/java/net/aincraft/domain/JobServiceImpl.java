package net.aincraft.domain;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.aincraft.util.DomainMapper;
import net.aincraft.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class JobServiceImpl implements JobService {

  private final DomainMapper<JobProgression, JobProgressionRecord> progressionMapper;
  private final DomainMapper<Job, JobRecord> jobMapper;
  private final DomainMapper<JobTask, JobTaskRecord> jobTaskMapper;
  private final Registry<ActionType> actionTypeRegistry;
  private final JobTaskRepository jobTaskRepository;
  private final KeyResolver keyResolver;
  private final JobRepository jobRepository;
  private final ProgressionService progressionService;

  @Inject
  public JobServiceImpl(
      DomainMapper<JobProgression, JobProgressionRecord> progressionMapper,
      DomainMapper<Job, JobRecord> jobMapper,
      DomainMapper<JobTask, JobTaskRecord> jobTaskMapper,
      Registry<ActionType> actionTypeRegistry,
      JobTaskRepository jobTaskRepository,
      KeyResolver keyResolver,
      JobRepository jobRepository, ProgressionService progressionService) {
    this.progressionMapper = progressionMapper;
    this.jobMapper = jobMapper;
    this.jobTaskMapper = jobTaskMapper;
    this.actionTypeRegistry = actionTypeRegistry;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.progressionService = progressionService;
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobRepository.getJobs().stream().map(jobMapper::toDomain).toList();
  }

  @Override
  public Job getJob(String jobKey) throws IllegalArgumentException {
    JobRecord record = jobRepository.load(jobKey);
    if (record == null) {
      throw new IllegalArgumentException();
    }
    return jobMapper.toDomain(record);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context) {
    Key contextKey = keyResolver.resolve(context);
    if (contextKey == null) {
      throw new IllegalStateException("No KeyResolver strategy registered for context type: " + context.getClass().getSimpleName());
    }
    JobTaskRecord record = jobTaskRepository.load(job.key().toString(), type.key().toString(),
        contextKey.toString());
    return jobTaskMapper.toDomain(record);
  }

  @Override
  public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
    Map<String, List<JobTaskRecord>> records = jobTaskRepository.getRecords(
        job.key().toString());
    Map<ActionType, List<JobTask>> domain = new LinkedHashMap<>();
    for (Entry<String, List<JobTaskRecord>> entry : records.entrySet()) {
      ActionType type = actionTypeRegistry.getOrThrow(NamespacedKey.fromString(entry.getKey()));
      List<JobTask> tasks = entry.getValue().stream()
          .map(jobTaskMapper::toDomain)
          .toList();
      domain.put(type, tasks);
    }
    return domain;
  }

  @Override
  public boolean update(JobProgression progression) {
    return progressionService.save(progressionMapper.toRecord(progression));
  }

  @Override
  public boolean joinJob(String playerId, String jobKey) throws IllegalArgumentException {
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      throw new IllegalArgumentException("failed to joined job, the job does not exist");
    }
    JobProgressionRecord record = progressionService.load(playerId, jobKey);
    if (progressionService.restore(playerId, jobKey)) {
      return true;
    }
    if (record == null) {
      return progressionService.save(
          new JobProgressionRecord(playerId, jobRecord, BigDecimal.ZERO));
    }
    return false;
  }

  @Override
  public boolean leaveJob(String playerId, String jobKey) throws IllegalArgumentException {
    JobProgressionRecord record = progressionService.load(playerId, jobKey);
    if (record == null) {
      return false;
    }
    return progressionService.archive(playerId,jobKey);
  }

  @Override
  public JobProgression getProgression(String playerId, String jobKey)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public List<JobProgression> getProgressions(OfflinePlayer player) {
    return progressionService.loadAllForPlayer(player.getUniqueId().toString(), 100).stream()
        .map(progressionMapper::toDomain).toList();
  }

  @Override
  public List<JobProgression> getProgressions(Key jobKey, int limit) {
    return progressionService.loadAllForJob(jobKey.toString(), limit).stream()
        .map(progressionMapper::toDomain).toList();
  }

  @Override
  public List<JobProgression> getArchivedProgressions(OfflinePlayer player) {
    return progressionService.loadAllArchivedForPlayer(player.getUniqueId().toString(), 100).stream()
        .map(progressionMapper::toDomain).toList();
  }
}
