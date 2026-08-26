package dev.mintychochip.payment;

import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Loads {@link RecipeExperienceDepreciationPolicy} from {@code config.yml}. */
public final class RecipeDepreciationSettings {

  private RecipeDepreciationSettings() {}

  /** From plugin. */
  public static @NotNull RecipeExperienceDepreciationPolicy fromPlugin(@NotNull Plugin plugin) {
    FileConfiguration config = plugin.getConfig();
    if (!config.isConfigurationSection("recipe-experience-depreciation")) {
      return RecipeExperienceDepreciationPolicy.defaults();
    }
    boolean enabled = config.getBoolean("recipe-experience-depreciation.enabled", true);
    int grace =
        config.getInt(
            "recipe-experience-depreciation.grace-levels",
            RecipeExperienceDepreciationPolicy.DEFAULT_GRACE_LEVELS);
    int window =
        config.getInt(
            "recipe-experience-depreciation.window-levels",
            RecipeExperienceDepreciationPolicy.DEFAULT_WINDOW_LEVELS);
    if (grace < 0) {
      plugin
          .getSLF4JLogger()
          .warn(
              "recipe-experience-depreciation.grace-levels must be >= 0 (got {}); using 0", grace);
      grace = 0;
    }
    if (window < 0) {
      plugin
          .getSLF4JLogger()
          .warn(
              "recipe-experience-depreciation.window-levels must be >= 0 (got {}); using default"
                  + " {}",
              window,
              RecipeExperienceDepreciationPolicy.DEFAULT_WINDOW_LEVELS);
      window = RecipeExperienceDepreciationPolicy.DEFAULT_WINDOW_LEVELS;
    }
    return new RecipeExperienceDepreciationPolicy(enabled, grace, window);
  }
}
