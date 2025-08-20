package net.aincraft.placeholders;

import com.google.inject.Inject;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.aincraft.service.ProgressionService;
import net.aincraft.util.KeyFactory;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ModularJobsPlaceholderExpansion extends PlaceholderExpansion {

  //TODO: set version dynamically
  private final ProgressionService progressionService;
  private final JobService jobService;
  private final KeyFactory keyFactory;
  private final String version = "1.1";

  @Inject
  ModularJobsPlaceholderExpansion(ProgressionService progressionService, JobService jobService,
      KeyFactory keyFactory) {
    this.progressionService = progressionService;
    this.jobService = jobService;
    this.keyFactory = keyFactory;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "modular";
  }

  @Override
  public @NotNull String getAuthor() {
    return "mintychochip";
  }

  @Override
  public @NotNull String getVersion() {
    return version;
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
    String[] args = params.split("_");
    if (args.length > 1) {
      if ("experience".equals(args[0])) {
        Optional<Job> job = jobService.getJob(keyFactory.create(args[1]));
        if (job.isEmpty()) {
          return "";
        }
        JobProgression progression = progressionService.get(player, job.get());
        if (progression == null) {
          return "";
        }
        return progression.getExperience().toPlainString();
      }
      if ("level".equals(args[1])) {

      }
    }
    return super.onRequest(player, params);
  }
}
