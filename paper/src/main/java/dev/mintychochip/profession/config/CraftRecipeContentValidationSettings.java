package dev.mintychochip.profession.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Loads craft-recipe cross-validation toggles from {@code config.yml}. */
public final class CraftRecipeContentValidationSettings {

  private static final String ROOT = "content-validation.craft-recipes";

  private CraftRecipeContentValidationSettings() {}

  /** Settings. */
  public record Settings(
      boolean enabled,
      boolean warnTasksWithoutRecipe,
      boolean logRecipesWithoutTask,
      int maxDetailLines) {

    /** Defaults. */
    public static Settings defaults() {
      return new Settings(true, true, true, 10);
    }
  }

  /** From plugin. */
  public static @NotNull Settings fromPlugin(@NotNull Plugin plugin) {
    return parse(plugin.getConfig(), plugin);
  }

  static @NotNull Settings parse(@NotNull FileConfiguration config) {
    if (!config.isConfigurationSection(ROOT)) {
      return Settings.defaults();
    }
    boolean enabled = config.getBoolean(ROOT + ".enabled", true);
    boolean warnTasksWithoutRecipe = config.getBoolean(ROOT + ".warn-tasks-without-recipe", true);
    boolean logRecipesWithoutTask = config.getBoolean(ROOT + ".log-recipes-without-task", true);
    int maxDetailLines = config.getInt(ROOT + ".max-detail-lines", 10);
    if (maxDetailLines < 0) {
      maxDetailLines = 0;
    }
    return new Settings(enabled, warnTasksWithoutRecipe, logRecipesWithoutTask, maxDetailLines);
  }

  static @NotNull Settings parse(@NotNull FileConfiguration config, @NotNull Plugin plugin) {
    if (!config.isConfigurationSection(ROOT)) {
      return Settings.defaults();
    }
    boolean enabled = config.getBoolean(ROOT + ".enabled", true);
    boolean warnTasksWithoutRecipe = config.getBoolean(ROOT + ".warn-tasks-without-recipe", true);
    boolean logRecipesWithoutTask = config.getBoolean(ROOT + ".log-recipes-without-task", true);
    int maxDetailLines = config.getInt(ROOT + ".max-detail-lines", 10);
    if (maxDetailLines < 0) {
      plugin
          .getSLF4JLogger()
          .warn("{}.max-detail-lines must be >= 0 (got {}); using 0", ROOT, maxDetailLines);
      maxDetailLines = 0;
    }
    return new Settings(enabled, warnTasksWithoutRecipe, logRecipesWithoutTask, maxDetailLines);
  }
}
