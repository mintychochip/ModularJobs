package net.aincraft.job;

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
import net.aincraft.container.Payable;
import net.aincraft.job.JobRecordRepository.JobRecord;
import net.aincraft.job.model.ActionTypeRecord;
import net.aincraft.job.model.JobProgressionRecord;
import net.aincraft.job.model.JobTaskRecord;
import net.aincraft.job.model.PayableRecord;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyResolver;
import net.aincraft.util.Mapper;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

final class JobServiceImpl implements JobService {

  private final Mapper<ActionType, ActionTypeRecord> actionTypeRecordMapper;
  private final Mapper<JobTask, JobTaskRecord> jobTaskRecordMapper;
  private final JobTaskRepository jobTaskRepository;
  private final Mapper<Job, JobRecord> jobRecordMapper;
  private final Mapper<Payable, PayableRecord> payableRecordMapper;
  private final Mapper<JobProgression, JobProgressionRecord> progressionRecordMapper;
  private final KeyResolver keyResolver;
  private final JobRecordRepository jobRecordRepository;
  private final JobsProgressionRepository progressionRepository;

  @Inject
  public JobServiceImpl(Mapper<ActionType, ActionTypeRecord> actionTypeRecordMapper,
      Mapper<JobTask, JobTaskRecord> jobTaskRecordMapper,
      JobTaskRepository jobTaskRepository, Mapper<Job, JobRecord> jobRecordMapper,
      Mapper<Payable, PayableRecord> payableRecordMapper,
      Mapper<JobProgression, JobProgressionRecord> progressionRecordMapper,
      KeyResolver keyResolver,
      JobRecordRepository jobRecordRepository,
      JobsProgressionRepository progressionRepository) {
    this.actionTypeRecordMapper = actionTypeRecordMapper;
    this.jobTaskRecordMapper = jobTaskRecordMapper;
    this.jobTaskRepository = jobTaskRepository;
    this.jobRecordMapper = jobRecordMapper;
    this.payableRecordMapper = payableRecordMapper;
    this.progressionRecordMapper = progressionRecordMapper;
    this.keyResolver = keyResolver;
    this.jobRecordRepository = jobRecordRepository;
    this.progressionRepository = progressionRepository;
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobRecordRepository.getJobs().stream().map(jobRecordMapper::toDomainObject).toList();
  }

  @Override
  public Optional<Job> getJob(Key jobKey) {
    return Optional.ofNullable(jobRecordRepository.getJob(jobKey.toString()))
        .map(jobRecordMapper::toDomainObject);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context) {
    Key contextKey = keyResolver.resolve(context);
    JobTaskRecord record = jobTaskRepository.getRecord(job.key().toString(), type.key().toString(),
        contextKey.toString());
    return jobTaskRecordMapper.toDomainObject(record);
  }

  @Override
  public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
    Map<ActionTypeRecord, List<JobTaskRecord>> records = jobTaskRepository.getRecords(
        job.key().toString());
    Map<ActionType, List<JobTask>> domain = new LinkedHashMap<>();
    for (Entry<ActionTypeRecord, List<JobTaskRecord>> entry : records.entrySet()) {
      ActionType type = actionTypeRecordMapper.toDomainObject(entry.getKey());
      List<JobTask> tasks = entry.getValue().stream().map(jobTaskRecordMapper::toDomainObject)
          .toList();
      domain.put(type, tasks);
    }
    return domain;
  }

  @Override
  public List<JobProgression> getProgressions(Key jobKey) {
    return progressionRepository.getRecords(jobKey.toString()).stream()
        .map(progressionRecordMapper::toDomainObject).toList();
  }
}
