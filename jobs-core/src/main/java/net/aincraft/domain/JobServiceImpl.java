package net.aincraft.domain;

import com.google.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import net.aincraft.service.JobService;
import net.aincraft.util.DomainMapper;
import net.aincraft.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

final class JobServiceImpl implements JobService {

  private final DomainMapper<JobProgression, JobProgressionRecord> progressionMapper;
  private final DomainMapper<Job, JobRecord> jobMapper;
  private final DomainMapper<JobTask, JobTaskRecord> jobTaskMapper;
  private final JobTaskRepository jobTaskRepository;
  private final KeyResolver keyResolver;
  private final JobRepository jobRepository;
  private final JobProgressionRepository progressionRepository;

  @Inject
  public JobServiceImpl(
      DomainMapper<JobProgression, JobProgressionRecord> progressionMapper,
      DomainMapper<Job, JobRecord> jobMapper,
      DomainMapper<JobTask, JobTaskRecord> jobTaskMapper,
      JobTaskRepository jobTaskRepository,
      KeyResolver keyResolver,
      JobRepository jobRepository,
      JobProgressionRepository progressionRepository) {
    this.progressionMapper = progressionMapper;
    this.jobMapper = jobMapper;
    this.jobTaskMapper = jobTaskMapper;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.progressionRepository = progressionRepository;
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobRepository.getJobs().stream().map(jobMapper::toDomainObject).toList();
  }

  @Override
  public Optional<Job> getJob(Key jobKey) {
    return Optional.ofNullable(jobRepository.getJob(jobKey.toString()))
        .map(jobMapper::toDomainObject);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context) {
    Key contextKey = keyResolver.resolve(context);
    JobTaskRecord record = jobTaskRepository.load(job.key().toString(), type.key().toString(),
        contextKey.toString());
    return jobTaskMapper.toDomainObject(record);
  }

  @Override
  public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
    Map<ActionTypeRecord, List<JobTaskRecord>> records = jobTaskRepository.getRecords(
        job.key().toString());
    Map<ActionType, List<JobTask>> domain = new LinkedHashMap<>();
    for (Entry<ActionTypeRecord, List<JobTaskRecord>> entry : records.entrySet()) {
      ActionType type = actionTypeRecordDomainMapper.toDomainObject(entry.getKey());
      List<JobTask> tasks = entry.getValue().stream().map(jobTaskRecordDomainMapper::toDomainObject)
          .toList();
      domain.put(type, tasks);
    }
    return domain;
  }

  @Override
  public List<JobProgression> getProgressions(Key jobKey, int limit) {
    return progressionRepository.loadAll(jobKey.toString(), limit).stream()
        .map(progressionMapper::toDomainObject).toList();
  }
}
