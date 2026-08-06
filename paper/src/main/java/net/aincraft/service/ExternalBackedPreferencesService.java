package net.aincraft.service;

import dev.jlo.preferences.api.Preference;
import dev.jlo.preferences.api.codec.PreferenceCodec;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ModularJobs {@link PreferencesService} facade backed by registered handles on the
 * external Preferences plugin API ({@code dev.jlo.preferences.api}).
 *
 * <p>Constructed only when that service is present; callers should go through
 * {@link PreferencesIntegration} so the optional dependency stays soft.
 */
public final class ExternalBackedPreferencesService implements PreferencesService {

  static final String ENTRIES_PER_PAGE_NAME = "entries-per-page";
  static final String GUI_MODE_NAME = "gui-mode";
  static final int MIN_ENTRIES = 1;
  static final int MAX_ENTRIES = 50;

  private final Plugin owner;
  private final @Nullable dev.jlo.preferences.api.PreferencesService external;
  private final Preference<Integer> entriesPerPage;
  private final Preference<Boolean> guiMode;
  private final int defaultEntriesPerPage;

  /**
   * Package-visible for unit tests that inject real {@link Preference} handles without a live
   * Bukkit service registration.
   */
  ExternalBackedPreferencesService(
      Plugin owner,
      @Nullable dev.jlo.preferences.api.PreferencesService external,
      Preference<Integer> entriesPerPage,
      Preference<Boolean> guiMode,
      int defaultEntriesPerPage) {
    this.owner = Objects.requireNonNull(owner, "owner");
    this.external = external; // null only for pure-handle unit tests
    this.entriesPerPage = Objects.requireNonNull(entriesPerPage, "entriesPerPage");
    this.guiMode = Objects.requireNonNull(guiMode, "guiMode");
    this.defaultEntriesPerPage = defaultEntriesPerPage;
  }

  /**
   * Registers ModularJobs player preferences on the external service and returns a facade.
   */
  public static ExternalBackedPreferencesService register(
      JavaPlugin plugin, dev.jlo.preferences.api.PreferencesService external) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(external, "external");

    int configuredEntries =
        plugin.getConfig().getInt("preferences.entries-per-page", 10);
    final int defaultEntries =
        Math.min(MAX_ENTRIES, Math.max(MIN_ENTRIES, configuredEntries));
    final boolean defaultGui =
        plugin.getConfig().getBoolean("preferences.default-gui-mode", true);

    Preference<Integer> entries =
        external.register(
            plugin,
            Integer.class,
            b ->
                b.playerScoped(ENTRIES_PER_PAGE_NAME)
                    .label(Component.text("Info entries per page"))
                    .description(
                        Component.text(
                            "How many job-info entries to show per page in chat/GUI"))
                    .codec(PreferenceCodec.integerSlider(MIN_ENTRIES, MAX_ENTRIES, 1))
                    .defaultValue(defaultEntries));

    Preference<Boolean> gui =
        external.register(
            plugin,
            Boolean.class,
            b ->
                b.playerScoped(GUI_MODE_NAME)
                    .label(Component.text("Job info GUI mode"))
                    .description(
                        Component.text("Prefer dialog GUI over chat for /jobs info"))
                    .codec(PreferenceCodec.booleanBox())
                    .defaultValue(defaultGui));

    return new ExternalBackedPreferencesService(plugin, external, entries, gui, defaultEntries);
  }

  Preference<Integer> entriesPerPagePreference() {
    return entriesPerPage;
  }

  Preference<Boolean> guiModePreference() {
    return guiMode;
  }

  /** Unregisters this plugin's preferences from the external service (disable path). */
  public void unregister() {
    if (external != null) {
      external.unregisterPlugin(owner);
    }
  }

  @Override
  public int getEntriesPerPage(@NotNull UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return getDefaultEntriesPerPage();
    }
    int value = entriesPerPage.get(player);
    if (value < MIN_ENTRIES) {
      return MIN_ENTRIES;
    }
    if (value > MAX_ENTRIES) {
      return MAX_ENTRIES;
    }
    return value;
  }

  @Override
  public void setEntriesPerPage(@NotNull UUID playerId, int entries) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return;
    }
    int clamped = entries;
    if (clamped < MIN_ENTRIES) {
      clamped = MIN_ENTRIES;
    }
    if (clamped > MAX_ENTRIES) {
      clamped = MAX_ENTRIES;
    }
    entriesPerPage.set(player, clamped);
  }

  @Override
  public int getDefaultEntriesPerPage() {
    return defaultEntriesPerPage;
  }

  @Override
  public boolean prefersGuiMode(@NotNull UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return getDefaultGuiModeFallback();
    }
    return Boolean.TRUE.equals(guiMode.get(player));
  }

  @Override
  public void setGuiMode(@NotNull UUID playerId, boolean guiModeValue) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return;
    }
    guiMode.set(player, guiModeValue);
  }

  private boolean getDefaultGuiModeFallback() {
    return Boolean.TRUE.equals(guiMode.defaultValue());
  }
}
