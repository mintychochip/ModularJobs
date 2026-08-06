package net.aincraft.service;

import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Soft integration with the Preferences plugin.
 *
 * <p>When the Preferences Bukkit service is registered, ModularJobs registers its player
 * preferences there and backs {@link PreferencesService} with those handles. When the plugin
 * or service is absent, falls back to the local in-memory {@link PreferencesServiceImpl}.
 *
 * <p>Types from {@code dev.jlo.preferences.api} are only referenced from
 * {@link ExternalBackedPreferencesService} and the private external path here, so this class
 * can load when Preferences is not installed (compileOnly dependency).
 */
public final class PreferencesIntegration {

  private PreferencesIntegration() {}

  /**
   * Result of wiring: facade for commands plus optional disable-path cleanup (unregister
   * from the external Preferences service).
   */
  public record Wiring(@NotNull PreferencesService service, @Nullable Runnable onDisable) {
    public Wiring {
      Objects.requireNonNull(service, "service");
    }
  }

  /**
   * Creates the ModularJobs preference facade, optionally wired to Preferences.
   */
  public static Wiring wire(@NotNull JavaPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");

    Plugin preferencesPlugin = Bukkit.getPluginManager().getPlugin("Preferences");
    if (preferencesPlugin == null || !preferencesPlugin.isEnabled()) {
      plugin.getSLF4JLogger().info(
          "Preferences plugin not present; using local preference defaults");
      return new Wiring(new PreferencesServiceImpl(plugin), null);
    }

    try {
      return wireExternal(plugin);
    } catch (NoClassDefFoundError | Exception e) {
      plugin.getLogger().log(
          Level.WARNING,
          "Failed to wire Preferences service; falling back to local preferences",
          e);
      return new Wiring(new PreferencesServiceImpl(plugin), null);
    }
  }

  /**
   * Loads the external service and registers ModularJobs prefs. Separated so linkage errors
   * when the API jar is not on the runtime classpath are catchable.
   */
  private static Wiring wireExternal(JavaPlugin plugin) {
    dev.jlo.preferences.api.PreferencesService external =
        Bukkit.getServicesManager().load(dev.jlo.preferences.api.PreferencesService.class);
    if (external == null) {
      plugin.getSLF4JLogger().info(
          "Preferences plugin enabled but PreferencesService is not registered; "
              + "using local preference defaults");
      return new Wiring(new PreferencesServiceImpl(plugin), null);
    }

    ExternalBackedPreferencesService bridged =
        ExternalBackedPreferencesService.register(plugin, external);
    plugin.getSLF4JLogger().info(
        "Registered ModularJobs preferences with Preferences plugin "
            + "(entries-per-page, gui-mode)");
    return new Wiring(bridged, bridged::unregister);
  }
}
