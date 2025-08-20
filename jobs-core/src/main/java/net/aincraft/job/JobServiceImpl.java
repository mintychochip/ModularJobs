package net.aincraft.job;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.aincraft.job.JobRecordRepository.JobRecord;
import net.aincraft.job.PayableRecordRepository.PayableRecord;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyResolver;
import net.aincraft.util.Mapper;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

final class JobServiceImpl implements JobService {

  private final Mapper<Job, JobRecord> jobRecordMapper;
  private final Mapper<Payable, PayableRecord> payableRecordMapper;
  private final KeyResolver keyResolver;
  private final JobRecordRepository jobRecordRepository;
  private final PayableRecordRepository payableRecordRepository;

  @Inject
  public JobServiceImpl(Mapper<Job, JobRecord> jobRecordMapper,
      Mapper<Payable, PayableRecord> payableRecordMapper,
      KeyResolver keyResolver,
      JobRecordRepository jobRecordRepository,
      PayableRecordRepository payableRecordRepository) {
    this.jobRecordMapper = jobRecordMapper;
    this.payableRecordMapper = payableRecordMapper;
    this.keyResolver = keyResolver;
    this.jobRecordRepository = jobRecordRepository;
    this.payableRecordRepository = payableRecordRepository;
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
  public List<Payable> getPayables(Job job, ActionType type, Context context) {
    Key key = keyResolver.resolve(context);
    return payableRecordRepository.getPayableRecords(
            job.key().toString(), type.key().toString(), key.toString()).stream()
        .map(payableRecordMapper::toDomainObject).toList();
  }
}
