package dev.mintychochip.placeholders;

import dev.mintychochip.service.JobService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * Soft-depend boundary for PlaceholderAPI. Always-loaded types (bootstrap / composition root) must
 * not reference {@code me.clip.placeholderapi} so the plugin classloads when PlaceholderAPI is
 * absent or incompatible.
 */
public interface PlaceholderExpansionHandle {

  /** Register. */
  void register();

  /** Unregister. */
  void unregister();

  /**
   * Returns a handle when PlaceholderAPI is enabled; otherwise null. Loads {@link
   * ModularJobsPlaceholderExpansion} only inside this method so the class (and its PAPI superclass)
   * is not resolved when PAPI is missing.
   */
  static @Nullable PlaceholderExpansionHandle tryCreate(JobService jobService) {
    if (Bukkit.getPluginManager() == null
        || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      return null;
    }
    ModularJobsPlaceholderExpansion expansion = new ModularJobsPlaceholderExpansion(jobService);
    return new PlaceholderExpansionHandle() {
      @Override
      public void register() {
        expansion.register();
      }

      @Override
      public void unregister() {
        expansion.unregister();
      }
    };
  }
}
