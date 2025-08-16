package net.aincraft.service;

import java.util.List;
import net.aincraft.Bridge;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.container.Context;

public interface JobTaskProvider {

  static JobTaskProvider jobTaskProvider() {
    return Bridge.bridge().jobTaskProvider();
  }

  boolean hasTask(Job job, ActionType type, Context context);

  JobTask getTask(Job job, ActionType type, Context context) throws IllegalArgumentException;

  void addTask(Job job, ActionType type, Context context, List<Payable> payables);
}
