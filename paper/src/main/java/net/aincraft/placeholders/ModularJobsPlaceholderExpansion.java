package net.aincraft.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing the player's ModularJobs progression.
 *
 * <p>Loaded only when PlaceholderAPI is available; requests read the backing
 * {@link JobService} and return an empty value when no progression exists.
 */
public final class ModularJobsPlaceholderExpansion extends PlaceholderExpansion {

  //TODO: set version dynamically
  private final JobService jobService;
  private final String version = "1.1";

  /**
   * Creates an expansion backed by the supplied job service.
   *
   * @param jobService service used to resolve placeholder progression values
   */
  public ModularJobsPlaceholderExpansion(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "modular";
  }

  @Override
  public @NotNull String getAuthor() {
    return "ModularJobs contributors";
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
        JobProgression progression = jobService.getProgression(player.getUniqueId().toString(),
            args[1]);
        if (progression == null) {
          return "";
        }
        return progression.experience().toPlainString();
      }
      if ("level".equals(args[1])) {

      }
    }
    return super.onRequest(player, params);
  }
}
