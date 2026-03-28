package net.aincraft.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of PreferencesService that stores player preferences.
 * Uses in-memory cache with config-based defaults.
 */
@Singleton
public class PreferencesServiceImpl implements PreferencesService {
  
  private final JavaPlugin plugin;
  private final Map<UUID, PlayerPreferences> preferencesCache = new HashMap<>();
  private static final int DEFAULT_ENTRIES_PER_PAGE = 10;
  private static final boolean DEFAULT_GUI_MODE = true;
  
  @Inject
  public PreferencesServiceImpl(JavaPlugin plugin) {
    this.plugin = plugin;
  }
  
  @Override
  public int getEntriesPerPage(@NotNull Player player) {
    PlayerPreferences prefs = preferencesCache.get(player.getUniqueId());
    if (prefs != null && prefs.entriesPerPage > 0) {
      return prefs.entriesPerPage;
    }
    return getDefaultEntriesPerPage();
  }
  
  @Override
  public void setEntriesPerPage(@NotNull Player player, int entries) {
    if (entries < 1) entries = 1;
    if (entries > 50) entries = 50;
    
    PlayerPreferences prefs = preferencesCache.computeIfAbsent(
        player.getUniqueId(), k -> new PlayerPreferences());
    prefs.entriesPerPage = entries;
  }
  
  @Override
  public int getDefaultEntriesPerPage() {
    FileConfiguration config = plugin.getConfig();
    return config.getInt("preferences.entries-per-page", DEFAULT_ENTRIES_PER_PAGE);
  }
  
  @Override
  public boolean prefersGuiMode(@NotNull Player player) {
    PlayerPreferences prefs = preferencesCache.get(player.getUniqueId());
    if (prefs != null) {
      return prefs.guiMode;
    }
    return DEFAULT_GUI_MODE;
  }
  
  @Override
  public void setGuiMode(@NotNull Player player, boolean guiMode) {
    PlayerPreferences prefs = preferencesCache.computeIfAbsent(
        player.getUniqueId(), k -> new PlayerPreferences());
    prefs.guiMode = guiMode;
  }
  
  /**
   * Clears cached preferences for a player (call on player quit).
   */
  public void clearPreferences(@NotNull UUID playerId) {
    preferencesCache.remove(playerId);
  }
  
  private static class PlayerPreferences {
    int entriesPerPage = DEFAULT_ENTRIES_PER_PAGE;
    boolean guiMode = DEFAULT_GUI_MODE;
  }
}
