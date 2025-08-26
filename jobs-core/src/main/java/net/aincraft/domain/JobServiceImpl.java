package net.aincraft.domain;

import com.google.inject.Inject;
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
  private final JobProgressionRepository progressionRepository;

  @Inject
  public JobServiceImpl(
      DomainMapper<JobProgression, JobProgressionRecord> progressionMapper,
      DomainMapper<Job, JobRecord> jobMapper,
      DomainMapper<JobTask, JobTaskRecord> jobTaskMapper,
      Registry<ActionType> actionTypeRegistry,
      JobTaskRepository jobTaskRepository,
      KeyResolver keyResolver,
      JobRepository jobRepository,
      JobProgressionRepository progressionRepository) {
    this.progressionMapper = progressionMapper;
    this.jobMapper = jobMapper;
    this.jobTaskMapper = jobTaskMapper;
    this.actionTypeRegistry = actionTypeRegistry;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.progressionRepository = progressionRepository;
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
    return progressionRepository.save(progressionMapper.toRecord(progression));
  }

  @Override
  public JobProgression getProgression(String playerId, String jobKey)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public List<JobProgression> getProgressions(OfflinePlayer player) {
    return List.of();
  }

  @Override
  public List<JobProgression> getProgressions(Key jobKey, int limit) {
    return progressionRepository.loadAll(jobKey.toString(), limit).stream()
        .map(progressionMapper::toDomain).toList();
  }
}
