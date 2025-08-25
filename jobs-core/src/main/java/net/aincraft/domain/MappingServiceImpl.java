package net.aincraft.domain;

import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.job.JobRecordRepository.JobRecord;
import net.aincraft.job.model.ActionTypeRecord;
import net.aincraft.job.model.JobProgressionRecord;
import net.aincraft.job.model.JobTaskRecord;
import net.aincraft.job.model.PayableRecord;
import net.aincraft.util.Mapper;

public class MappingServiceImpl implements MappingService {

  @Override
  public Mapper<ActionType, ActionTypeRecord> actionType() {
    return null;
  }

  @Override
  public Mapper<JobTask, JobTaskRecord> jobTask() {
    return null;
  }

  @Override
  public Mapper<Job, JobRecord> job() {
    return null;
  }

  @Override
  public Mapper<Payable, PayableRecord> payable() {
    return null;
  }

  @Override
  public Mapper<JobProgression, JobProgressionRecord> jobProgression() {
    return null;
  }
}
