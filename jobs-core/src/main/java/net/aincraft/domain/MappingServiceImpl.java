package net.aincraft.domain;

import com.google.inject.Inject;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.ActionTypeRecord;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.util.Mapper;

final class MappingServiceImpl implements MappingService {

  private final Mapper<ActionType,ActionTypeRecord> actionTypeMapper;
  private final Mapper<JobTask,JobTaskRecord> jobTaskMapper;
  private final Mapper<Job,JobRecord> jobMapper;
  private final Mapper<Payable,PayableRecord> payableMapper;
  private final Mapper<JobProgression,JobProgressionRecord> jobProgressionMapper;

  @Inject
  public MappingServiceImpl(Mapper<ActionType, ActionTypeRecord> actionTypeMapper,
      Mapper<JobTask, JobTaskRecord> jobTaskMapper, Mapper<Job, JobRecord> jobMapper,
      Mapper<Payable, PayableRecord> payableMapper,
      Mapper<JobProgression, JobProgressionRecord> jobProgressionMapper) {
    this.actionTypeMapper = actionTypeMapper;
    this.jobTaskMapper = jobTaskMapper;
    this.jobMapper = jobMapper;
    this.payableMapper = payableMapper;
    this.jobProgressionMapper = jobProgressionMapper;
  }

  @Override
  public Mapper<ActionType, ActionTypeRecord> actionType() {
    return actionTypeMapper;
  }

  @Override
  public Mapper<JobTask, JobTaskRecord> jobTask() {
    return jobTaskMapper;
  }

  @Override
  public Mapper<Job, JobRecord> job() {
    return jobMapper;
  }

  @Override
  public Mapper<Payable, PayableRecord> payable() {
    return payableMapper;
  }

  @Override
  public Mapper<JobProgression, JobProgressionRecord> jobProgression() {
    return jobProgressionMapper;
  }
}
