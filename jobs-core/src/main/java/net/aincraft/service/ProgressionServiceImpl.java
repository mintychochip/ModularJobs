package net.aincraft.service;

import com.google.inject.Inject;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.repository.ProgressionRepository;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProgressionServiceImpl implements ProgressionService {

  private final ProgressionRepository progressionRepository;

  @Inject
  ProgressionServiceImpl(ProgressionRepository progressionRepository) {
    this.progressionRepository = progressionRepository;
  }

  @Override
  public @NotNull JobProgression create(OfflinePlayer player, Job job)
      throws IllegalArgumentException {
    return progressionRepository.create(player.getUniqueId(),job.key());
  }

  @Override
  public @Nullable JobProgression get(OfflinePlayer player, Job job) {
    return progressionRepository.get(player.getUniqueId(),job.key());
  }

  @Override
  public @NotNull List<JobProgression> getAll(OfflinePlayer player) {
    return progressionRepository.getAllProgressions(player.getUniqueId());
  }

  @Override
  public void update(JobProgression progression) {
    progressionRepository.update(progression);
  }
}
