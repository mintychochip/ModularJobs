package net.aincraft.service;

import java.util.List;
import net.aincraft.Bridge;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ProgressionService {

  static ProgressionService progressionService() {
    return Bridge.bridge().progressionService();
  }

  @NotNull
  JobProgression create(OfflinePlayer player, Job job) throws IllegalArgumentException;

  @Nullable
  JobProgression get(OfflinePlayer player, Job job);

  @NotNull
  List<JobProgression> getAll(OfflinePlayer player);

  void update(JobProgression progression);

}
