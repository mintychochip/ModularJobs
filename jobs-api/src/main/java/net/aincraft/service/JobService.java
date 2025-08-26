package net.aincraft.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;


public interface JobService {

  @NotNull
  List<Job> getJobs();

  Job getJob(String jobKey) throws IllegalArgumentException;

  JobTask getTask(Job job, ActionType type, Context context);

  Map<ActionType, List<JobTask>> getAllTasks(Job job);

  boolean update(JobProgression progression);

  JobProgression getProgression(String playerId, String jobKey) throws IllegalArgumentException;

  List<JobProgression> getProgressions(OfflinePlayer player);

  List<JobProgression> getProgressions(Key jobKey, int limit);
}
