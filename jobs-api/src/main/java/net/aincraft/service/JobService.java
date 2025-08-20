package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;


public interface JobService {

  @NotNull
  List<Job> getJobs();

  Optional<Job> getJob(Key jobKey);

  List<Payable> getPayables(Job job, ActionType type, Context context);

}
