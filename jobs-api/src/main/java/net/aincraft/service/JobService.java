package net.aincraft.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;


public interface JobService {

  @NotNull
  List<Job> getJobs();

  Optional<Job> getJob(Key jobKey);

  JobTask getTask(Job job, ActionType type, Context context);

  Map<ActionType, List<JobTask>> getAllTasks(Job job);

  List<JobProgression> getProgressions(Key jobKey);
}
