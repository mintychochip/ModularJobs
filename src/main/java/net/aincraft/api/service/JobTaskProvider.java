package net.aincraft.api.service;

import java.io.IOException;
import java.util.List;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.context.Context;

public interface JobTaskProvider {
  JobTask getTask(Job job, ActionType type, Context context) throws IOException;
  void addTask(Job job, ActionType type, Context object, List<Payable> payables);
}
