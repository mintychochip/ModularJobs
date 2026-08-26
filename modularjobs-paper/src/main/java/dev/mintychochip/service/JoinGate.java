package dev.mintychochip.service;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.config.ProgressionLimitsConfig;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Enforces join eligibility for {@code /jobs join} and the browse GUI.
 *
 * <p>Each rule is independent: when {@code maxJobs} is positive, joining beyond the limit is
 * rejected; when the player lacks {@code jobs.join.<job>} permission, joining is rejected; when
 * world join restriction is enabled, a job cannot be joined while in a world that pays nothing
 * (i.e. listed in the payout disabled-worlds set).
 *
 * <p>Outcome of a membership check.
 */
public final class JoinGate {

  /** Outcome of a join-eligibility check. */
  public enum JoinResult {
    ALLOWED,
    MAX_JOBS,
    PERMISSION_DENIED,
    WORLD_DENIED
  }

  private final ProgressionLimitsConfig config;
  private final Set<String> disabledWorlds;

  /**
   * Creates a join eligibility gate using progression and world restrictions.
   *
   * @param config join-eligibility knobs
   * @param disabledWorlds case-insensitive payout disabled-world names (from {@code
   *     PaymentSettings.disabledWorlds()})
   */
  public JoinGate(@NotNull ProgressionLimitsConfig config, @NotNull Set<String> disabledWorlds) {
    this.config = config;
    this.disabledWorlds = disabledWorlds;
  }

  /**
   * Checks whether {@code player} may join {@code job} given their current jobs.
   *
   * @param player the joining player
   * @param job the job being joined
   * @param current the player's current (non-archived) progressions
   * @return the first violated rule, or {@link JoinResult#ALLOWED}
   */
  public @NotNull JoinResult canJoin(
      @NotNull Player player, @NotNull Job job, @NotNull List<JobProgression> current) {
    if (config.maxJobs() > 0 && current.size() >= config.maxJobs()) {
      return JoinResult.MAX_JOBS;
    }
    if (!player.hasPermission("jobs.join." + job.getPlainName().toLowerCase(Locale.ROOT))) {
      return JoinResult.PERMISSION_DENIED;
    }
    if (config.worldJoinRestriction()
        && player.getWorld() != null
        && disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
      return JoinResult.WORLD_DENIED;
    }
    return JoinResult.ALLOWED;
  }
}
