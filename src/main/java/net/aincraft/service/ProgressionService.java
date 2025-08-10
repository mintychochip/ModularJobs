package net.aincraft.service;

import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import org.bukkit.OfflinePlayer;

public interface ProgressionService {

  void create(OfflinePlayer player, Job job);

  JobProgression get(OfflinePlayer player);

  void update(OfflinePlayer player, JobProgression progression);

  void delete(OfflinePlayer player);

  boolean exists(OfflinePlayer player);
}
