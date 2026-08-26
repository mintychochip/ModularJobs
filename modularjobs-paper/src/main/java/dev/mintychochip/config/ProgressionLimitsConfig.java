package dev.mintychochip.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Join-eligibility knobs loaded from {@code config.yml} under {@code progression-limits}.
 *
 * <p>{@code maxJobs == 0} means unlimited concurrent jobs. {@code autoJoinJobs} are joined
 * automatically on login when the player is not already in them. {@code worldJoinRestriction} gates
 * joins to the player's current world via the payout disabled-worlds rule (a job cannot be joined
 * while in a world that pays nothing).
 */
public record ProgressionLimitsConfig(
    int maxJobs, @NotNull List<String> autoJoinJobs, boolean worldJoinRestriction) {

  /** Defaults. */
  public static ProgressionLimitsConfig defaults() {
    return new ProgressionLimitsConfig(0, List.of(), true);
  }

  /** Parses from a raw map (used by tests and {@link #fromPlugin}). */
  public static ProgressionLimitsConfig fromMap(@NotNull Map<?, ?> source) {
    int maxJobs = 0;
    Object rawMax = source.get("max-jobs");
    if (rawMax instanceof Number n) {
      maxJobs = Math.max(0, n.intValue());
    }
    List<String> autoJoin = new ArrayList<>();
    Object rawAuto = source.get("auto-join-jobs");
    if (rawAuto instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof String s && !s.isBlank()) {
          autoJoin.add(s.toLowerCase(Locale.ROOT));
        }
      }
    }
    boolean worldJoin = true;
    Object rawWorld = source.get("world-join-restriction");
    if (rawWorld instanceof Boolean b) {
      worldJoin = b;
    }
    return new ProgressionLimitsConfig(maxJobs, List.copyOf(autoJoin), worldJoin);
  }

  /** Loads from {@code config.yml}. */
  public static ProgressionLimitsConfig fromPlugin(@NotNull Plugin plugin) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression-limits");
    if (section == null) {
      return defaults();
    }
    return fromMap(section.getValues(false));
  }
}
