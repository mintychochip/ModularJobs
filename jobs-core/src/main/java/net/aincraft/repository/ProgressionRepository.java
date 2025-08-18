package net.aincraft.repository;

import java.util.List;
import java.util.UUID;
import net.aincraft.JobProgression;
import net.kyori.adventure.key.Key;

public interface ProgressionRepository {

  JobProgression create(UUID uuid, Key jobKey);

  void update(JobProgression progression);

  JobProgression get(UUID uuid, Key jobKey);

  List<JobProgression> getAllProgressions(UUID playerId);

}
