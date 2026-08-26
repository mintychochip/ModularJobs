package dev.mintychochip.service;

import dev.mintychochip.payable.ExperienceBarColorPreference;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Soft integration with the external Preferences plugin.
 *
 * <p>When the Preferences Bukkit service is registered, ModularJobs registers its per-player XP
 * boss bar color preference there. When the plugin or service is absent, yields a null preference
 * so {@link dev.mintychochip.payable.ExperienceBarColorProvider} falls back to green.
 */
@NullMarked
public final class PreferencesIntegration {

  static final String EXPERIENCE_BAR_COLOR_NAME = "experience-bar-color";

  private PreferencesIntegration() {}

  /** Result of wiring: optional preference handle plus optional disable-path cleanup. */
  public record Wiring(
      @Nullable ExperienceBarColorPreference experienceBarColor, @Nullable Runnable onDisable) {
    // Both fields are intentionally nullable: the absent-service paths construct
    // Wiring(null, null).
  }

  /**
   * Loads the external Preferences service and registers ModularJobs' XP bar color preference.
   * Returns a {@link Wiring} with null fields when the service is unavailable.
   */
  public static Wiring wire(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");

    try {
      return ExternalPreferencesBridge.wire(plugin);
    } catch (NoClassDefFoundError e) {
      plugin
          .getSLF4JLogger()
          .info("Preferences API not present; XP boss bar color stays default green");
      return new Wiring(null, null);
    } catch (IllegalArgumentException | IllegalStateException e) {
      plugin
          .getLogger()
          .log(
              Level.WARNING,
              "Failed to register XP bar color preference; falling back to green",
              e);
      return new Wiring(null, null);
    }
  }
}
