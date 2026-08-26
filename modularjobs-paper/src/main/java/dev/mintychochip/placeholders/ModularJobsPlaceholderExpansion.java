package dev.mintychochip.placeholders;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing the player's ModularJobs progression.
 *
 * <p>Loaded only when PlaceholderAPI is available; requests read the backing {@link JobService} and
 * return an empty value when no progression exists or the placeholder is unknown.
 *
 * <p>Supported placeholders (identifier {@code modular}):
 *
 * <ul>
 *   <li>{@code joinedjobcount}, {@code jobs}, {@code totallevels}, {@code maxjobs}, {@code
 *       archivedjobs}
 *   <li>{@code level_<job>}, {@code experience_<job>}, {@code maxexperience_<job>}, {@code
 *       maxlevel_<job>}, {@code name_<job>}, {@code description_<job>}, {@code isin_<job>}, {@code
 *       canjoin_<job>}
 * </ul>
 */
public final class ModularJobsPlaceholderExpansion extends PlaceholderExpansion {

  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private final JobService jobService;
  private static final String VERSION = "1.2";

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
    return VERSION;
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
    if (player == null) {
      return "";
    }
    UUID playerId = player.getUniqueId();
    String lower = params.toLowerCase(Locale.ROOT);

    // Player-level placeholders
    switch (lower) {
      case "joinedjobcount" -> {
        return Integer.toString(jobService.getProgressions(playerId).size());
      }
      case "jobs" -> {
        List<JobProgression> progressions = jobService.getProgressions(playerId);
        StringBuilder sb = new StringBuilder();
        for (JobProgression p : progressions) {
          if (sb.length() > 0) {
            sb.append(',');
          }
          sb.append(p.job().getPlainName());
        }
        return sb.toString();
      }
      case "totallevels" -> {
        int total = 0;
        for (JobProgression p : jobService.getProgressions(playerId)) {
          total += p.level();
        }
        return Integer.toString(total);
      }
      case "maxjobs" -> {
        return Integer.toString(jobService.getJobs().size());
      }
      case "archivedjobs" -> {
        return Integer.toString(jobService.getArchivedProgressions(playerId).size());
      }
      default -> {
        // fall through to job-level parsing
      }
    }

    // Job-level placeholders: <param>_<job>
    int underscore = lower.indexOf('_');
    if (underscore <= 0 || underscore == lower.length() - 1) {
      return "";
    }
    String param = lower.substring(0, underscore);
    String jobName = lower.substring(underscore + 1);
    JobProgression progression = progressionFor(playerId, jobName);
    if (progression == null) {
      return switch (param) {
        case "isin" -> "false";
        case "canjoin" -> "true";
        default -> "";
      };
    }
    Job job = progression.job();
    return switch (param) {
      case "level" -> Integer.toString(progression.level());
      case "experience" -> progression.experience().toPlainString();
      case "maxexperience" -> maxExperience(progression);
      case "maxlevel" -> Integer.toString(job.maxLevel());
      case "name" -> job.getPlainName();
      case "description" -> PLAIN.serialize(job.description());
      case "isin" -> "true";
      case "canjoin" -> "false";
      default -> "";
    };
  }

  private JobProgression progressionFor(UUID playerId, String jobName) {
    for (Job job : jobService.getJobs()) {
      if (job.getPlainName().equalsIgnoreCase(jobName)) {
        return jobService.getProgression(playerId.toString(), job.key().toString());
      }
    }
    // Fallback: treat the token as a raw key suffix (e.g. modularjobs:miner)
    try {
      return jobService.getProgression(playerId.toString(), "modularjobs:" + jobName);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String maxExperience(JobProgression progression) {
    int level = progression.level();
    BigDecimal forNext = progression.experienceForLevel(level + 1);
    if (forNext == null) {
      return progression.experience().toPlainString();
    }
    return forNext.toPlainString();
  }
}
