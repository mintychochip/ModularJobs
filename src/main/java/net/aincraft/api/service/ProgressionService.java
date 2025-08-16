package net.aincraft.api.service;

import java.util.List;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
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

  void update(List<? extends JobProgression> progressions);

}
