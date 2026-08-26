package dev.mintychochip.service;

import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.codec.PreferenceCodec;
import dev.mintychochip.preferences.api.PreferencesService;
import java.util.Objects;
import java.util.logging.Level;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Soft integration with the external Preferences plugin.
 *
 * <p>When the Preferences Bukkit service is registered, ModularJobs registers its per-player
 * XP boss bar color preference there. When the plugin or service is absent, yields a null
 * preference so {@link dev.mintychochip.payable.ExperienceBarColorProvider} falls back to green.
 */
@NullMarked
public final class PreferencesIntegration {

  static final String EXPERIENCE_BAR_COLOR_NAME = "experience-bar-color";

  private PreferencesIntegration() {}

  /** Result of wiring: optional preference handle plus optional disable-path cleanup. */
  public record Wiring(
      @Nullable Preference<Color> experienceBarColor, @Nullable Runnable onDisable) {
    // Both fields are intentionally nullable: the absent-service paths construct Wiring(null, null).
  }

  /**
   * Loads the external Preferences service and registers ModularJobs' XP bar color preference.
   * Returns a {@link Wiring} with null fields when the service is unavailable.
   */
  public static Wiring wire(JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");

    // The Preferences plugin registers its service at enable time, so service presence is the
    // authoritative capability check. No plugin-name gate: the test registers the service without
    // an enabled "Preferences" plugin fixture.
    PreferencesService external = Bukkit.getServicesManager().load(PreferencesService.class);
    if (external == null) {
      plugin.getSLF4JLogger().info(
          "Preferences plugin not present; XP boss bar color stays default green");
      return new Wiring(null, null);
    }

    try {
      Preference<Color> color =
          external.register(
              plugin,
              Color.class,
              b ->
                  b.playerScoped(EXPERIENCE_BAR_COLOR_NAME)
                      .label(Component.text("XP bar color"))
                      .description(Component.text("Color of your job experience boss bar"))
                      .codec(PreferenceCodec.enumerated(Color.class, c -> Component.text(c.name())))
                      .defaultValue(Color.GREEN));
      plugin.getSLF4JLogger().info(
          "Registered ModularJobs XP boss bar color preference with Preferences plugin");
      return new Wiring(color, () -> external.unregisterPlugin(plugin));
    } catch (RuntimeException e) {
      plugin.getLogger().log(
          Level.WARNING, "Failed to register XP bar color preference; falling back to green", e);
      return new Wiring(null, null);
    }
  }
}
