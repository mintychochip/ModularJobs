package net.aincraft.api.service;

import java.util.List;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.Payable;
import net.aincraft.api.context.Context;

public interface JobTaskProvider {

  static JobTaskProvider jobTaskProvider() {
    return Bridge.bridge().jobTaskProvider();
  }

  boolean hasTask(Job job, ActionType type, Context context);

  JobTask getTask(Job job, ActionType type, Context context) throws IllegalArgumentException;

  void addTask(Job job, ActionType type, Context context, List<Payable> payables);
}
