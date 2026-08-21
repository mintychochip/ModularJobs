package dev.mintychochip.payment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Payment rule knobs loaded from {@code config.yml}. */
public record PaymentSettings(
    boolean payInCreative,
    boolean payWhileRiding,
    @NotNull Set<String> disabledWorlds,
    double killContributionCutoff,
    double furnaceMaxDistance) {

  public static final double DEFAULT_KILL_CONTRIBUTION_CUTOFF = 0.5;
  public static final double DEFAULT_FURNACE_MAX_DISTANCE = 25.0;

  /**
   * Default settings: creative pay enabled, riding pay disabled, no disabled worlds, and default
   * kill-contribution cutoff / furnace distance.
   */
  public static PaymentSettings defaults() {
    return new PaymentSettings(
        true, false, Set.of(), DEFAULT_KILL_CONTRIBUTION_CUTOFF, DEFAULT_FURNACE_MAX_DISTANCE);
  }

  /**
   * Loads payment settings from {@code config.yml}, validating ranges and normalizing disabled
   * world names to lowercase.
   */
  public static PaymentSettings fromPlugin(@NotNull Plugin plugin) {
    FileConfiguration config = plugin.getConfig();
    final boolean payInCreative = config.getBoolean("pay-in-creative", true);
    final boolean payWhileRiding = config.getBoolean("pay-while-riding", false);

    List<String> worlds = config.getStringList("disabled-worlds");
    Set<String> disabled = new HashSet<>();
    for (String world : worlds) {
      if (world != null && !world.isBlank()) {
        disabled.add(world.toLowerCase(Locale.ROOT));
      }
    }

    double cutoff = config.getDouble("kill-contribution-cutoff", DEFAULT_KILL_CONTRIBUTION_CUTOFF);
    if (cutoff < 0.0 || cutoff > 1.0) {
      plugin
          .getSLF4JLogger()
          .warn(
              "kill-contribution-cutoff must be between 0 and 1 (got {}); using default {}",
              cutoff,
              DEFAULT_KILL_CONTRIBUTION_CUTOFF);
      cutoff = DEFAULT_KILL_CONTRIBUTION_CUTOFF;
    }

    double furnaceDistance = config.getDouble("furnace-max-distance", DEFAULT_FURNACE_MAX_DISTANCE);
    if (furnaceDistance < 0.0) {
      plugin
          .getSLF4JLogger()
          .warn(
              "furnace-max-distance must be >= 0 (got {}); using default {}",
              furnaceDistance,
              DEFAULT_FURNACE_MAX_DISTANCE);
      furnaceDistance = DEFAULT_FURNACE_MAX_DISTANCE;
    }

    return new PaymentSettings(
        payInCreative,
        payWhileRiding,
        Collections.unmodifiableSet(disabled),
        cutoff,
        furnaceDistance);
  }

  /**
   * Reports whether {@code worldName} is in the disabled-worlds set.
   *
   * @return true when {@code worldName} (case-insensitive) is in the disabled-worlds set
   */
  public boolean isWorldDisabled(@NotNull String worldName) {
    return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
  }

  /** Squared distance for furnace proximity checks (avoids sqrt). */
  public double furnaceMaxDistanceSquared() {
    return furnaceMaxDistance * furnaceMaxDistance;
  }
}
