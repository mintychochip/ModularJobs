package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;

public interface JobTaskProvider {

  Optional<JobTask> getTask(Job job, ActionType type, Context context);

  void addTask(Job job, ActionType type, Context context, List<Payable> payables);
}
