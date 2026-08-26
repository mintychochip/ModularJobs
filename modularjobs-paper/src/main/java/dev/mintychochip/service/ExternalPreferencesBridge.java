package dev.mintychochip.service;

import dev.mintychochip.payable.ExperienceBarColorPreference;
import dev.mintychochip.preferences.api.Preference;
import dev.mintychochip.preferences.api.PreferencesService;
import dev.mintychochip.preferences.api.codec.PreferenceCodec;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/**
 * Sole holder of external Preferences API references. When {@code preferences-api} classes are
 * absent at runtime, loading this class fails with {@link NoClassDefFoundError}; callers must catch
 * that at the integration boundary.
 */
@NullMarked
final class ExternalPreferencesBridge {

  private ExternalPreferencesBridge() {}

  static PreferencesIntegration.Wiring wire(JavaPlugin plugin) {
    PreferencesService external = Bukkit.getServicesManager().load(PreferencesService.class);
    if (external == null) {
      plugin
          .getSLF4JLogger()
          .info("Preferences plugin not present; XP boss bar color stays default green");
      return new PreferencesIntegration.Wiring(null, null);
    }

    Preference<Color> color =
        external.register(
            plugin,
            Color.class,
            b ->
                b.playerScoped(PreferencesIntegration.EXPERIENCE_BAR_COLOR_NAME)
                    .label(Component.text("XP bar color"))
                    .description(Component.text("Color of your job experience boss bar"))
                    .codec(PreferenceCodec.enumerated(Color.class, c -> Component.text(c.name())))
                    .defaultValue(Color.GREEN));
    plugin
        .getSLF4JLogger()
        .info("Registered ModularJobs XP boss bar color preference with Preferences plugin");
    ExperienceBarColorPreference preference = player -> color.get(player);
    return new PreferencesIntegration.Wiring(preference, () -> external.unregisterPlugin(plugin));
  }
}
