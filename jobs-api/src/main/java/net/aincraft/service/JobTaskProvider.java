package net.aincraft.service;

import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;

public interface JobTaskProvider {

  Optional<JobTask> getTask(Job job, ActionType type, Context context);

}
