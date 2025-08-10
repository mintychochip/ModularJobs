package net.aincraft.service;

import java.util.List;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import org.bukkit.OfflinePlayer;

public interface ProgressionService {

  static ProgressionService progressionService() {
    return Bridge.bridge().progressionService();
  }

  JobProgression create(OfflinePlayer player, Job job);

  JobProgression get(OfflinePlayer player, Job job);

  List<JobProgression> getAll(OfflinePlayer player);

  void update(OfflinePlayer player, Job job, long experience);

  void delete(OfflinePlayer player);

  boolean exists(OfflinePlayer player);
}
