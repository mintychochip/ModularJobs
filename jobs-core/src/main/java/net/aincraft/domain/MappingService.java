package net.aincraft.domain;

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

public interface MappingService {

  Mapper<ActionType, ActionTypeRecord> actionType();

  Mapper<JobTask, JobTaskRecord> jobTask();

  Mapper<Job, JobRecord> job();

  Mapper<Payable, PayableRecord> payable();

  Mapper<JobProgression, JobProgressionRecord> jobProgression();
}
