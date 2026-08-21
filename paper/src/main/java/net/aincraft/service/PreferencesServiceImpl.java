package net.aincraft.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of PreferencesService that stores player preferences.
 * Uses in-memory cache with config-based defaults.
 */
public class PreferencesServiceImpl implements PreferencesService {
  
  private final JavaPlugin plugin;
  private final Map<UUID, PlayerPreferences> preferencesCache = new HashMap<>();
  private static final int DEFAULT_ENTRIES_PER_PAGE = 10;
  private static final boolean DEFAULT_GUI_MODE = true;
  
  public PreferencesServiceImpl(JavaPlugin plugin) {
    this.plugin = plugin;
  }
  
  @Override
  public int getEntriesPerPage(@NotNull UUID playerId) {
    PlayerPreferences prefs = preferencesCache.get(playerId);
    if (prefs != null && prefs.entriesPerPage > 0) {
      return prefs.entriesPerPage;
    }
    return getDefaultEntriesPerPage();
  }
  
  @Override
  public void setEntriesPerPage(@NotNull UUID playerId, int entries) {
    if (entries < 1) {
      entries = 1;
    }
    if (entries > 50) {
      entries = 50;
    }
    
    PlayerPreferences prefs = preferencesCache.computeIfAbsent(
        playerId, k -> new PlayerPreferences());
    prefs.entriesPerPage = entries;
  }
  
  @Override
  public int getDefaultEntriesPerPage() {
    FileConfiguration config = plugin.getConfig();
    return config.getInt("preferences.entries-per-page", DEFAULT_ENTRIES_PER_PAGE);
  }
  
  @Override
  public boolean prefersGuiMode(@NotNull UUID playerId) {
    PlayerPreferences prefs = preferencesCache.get(playerId);
    if (prefs != null && prefs.guiModeSet) {
      return prefs.guiMode;
    }
    return getDefaultGuiMode();
  }
  
  @Override
  public void setGuiMode(@NotNull UUID playerId, boolean guiMode) {
    PlayerPreferences prefs = preferencesCache.computeIfAbsent(
        playerId, k -> new PlayerPreferences());
    prefs.guiMode = guiMode;
    prefs.guiModeSet = true;
  }

  /** Config default for GUI mode ({@code preferences.default-gui-mode}). */
  public boolean getDefaultGuiMode() {
    return plugin.getConfig().getBoolean("preferences.default-gui-mode", DEFAULT_GUI_MODE);
  }
  
  /**
   * Clears cached preferences for a player (call on player quit).
   */
  public void clearPreferences(@NotNull UUID playerId) {
    preferencesCache.remove(playerId);
  }
  
  private static class PlayerPreferences {
    int entriesPerPage = 0;
    boolean guiMode = DEFAULT_GUI_MODE;
    boolean guiModeSet = false;
  }
}
