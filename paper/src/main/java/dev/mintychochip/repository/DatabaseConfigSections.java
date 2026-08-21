package dev.mintychochip.repository;

import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates {@code database.yml} (or any ConfigurationSection root) keys used for
 * ConnectionSource wiring. Fails with a clear IllegalArgumentException rather than NPE.
 */
public final class DatabaseConfigSections {

  private DatabaseConfigSections() {}

  /**
   * Require that {@code path} exists and is a nested configuration section (map).
   *
   * @throws IllegalArgumentException if missing, null root, or non-section value
   */
  @NotNull
  public static ConfigurationSection requireSection(
      @Nullable ConfigurationSection root, @NotNull String path) {
    Objects.requireNonNull(path, "path");
    if (root == null) {
      throw new IllegalArgumentException(
          "database.yml root is null; cannot read section '" + path + "'");
    }
    if (!root.contains(path)) {
      Set<String> keys = root.getKeys(false);
      throw new IllegalArgumentException(
          "database.yml missing required section '" + path + "'. Available keys: " + keys);
    }
    ConfigurationSection section = root.getConfigurationSection(path);
    if (section == null) {
      Object raw = root.get(path);
      String type = raw == null ? "null" : raw.getClass().getSimpleName();
      throw new IllegalArgumentException(
          "database.yml key '" + path
              + "' must be a configuration section (map), not a scalar/list (got "
              + type + ")");
    }
    return section;
  }

  /**
   * Prefer {@code preferredPath} if present as a section; otherwise return {@code fallback}.
   * If preferred path exists but is not a section, fails clearly (does not silently fall back).
   */
  @NotNull
  public static ConfigurationSection sectionOrFallback(
      @Nullable ConfigurationSection root,
      @NotNull String preferredPath,
      @NotNull ConfigurationSection fallback) {
    Objects.requireNonNull(preferredPath, "preferredPath");
    Objects.requireNonNull(fallback, "fallback");
    if (root == null || !root.contains(preferredPath)) {
      return fallback;
    }
    ConfigurationSection section = root.getConfigurationSection(preferredPath);
    if (section == null) {
      Object raw = root.get(preferredPath);
      String type = raw == null ? "null" : raw.getClass().getSimpleName();
      throw new IllegalArgumentException(
          "database.yml key '" + preferredPath
              + "' must be a configuration section (map), not a scalar/list (got "
              + type + ")");
    }
    return section;
  }
}
