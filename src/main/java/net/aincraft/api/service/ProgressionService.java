package net.aincraft.api.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.JobProgressionView;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ProgressionService {

  static ProgressionService progressionService() {
    return Bridge.bridge().progressionService();
  }

  @NotNull
  JobProgression create(OfflinePlayer player, Job job);

  @Nullable
  JobProgression get(OfflinePlayer player, Job job);

  @NotNull
  List<JobProgression> getAll(OfflinePlayer player);

  void update(JobProgressionView progression);

  void update(List<? extends JobProgressionView> progressions);

  void delete(OfflinePlayer player);
}
