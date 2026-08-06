package net.aincraft.service;

import org.bukkit.entity.Player;

/**
 * Service for managing player preferences.
 * Controls settings like entries per page for info displays.
 */
public interface PreferencesService {
  
  /**
   * Gets the number of entries to display per page for the given player.
   * @param player The player to get the preference for
   * @return The number of entries per page (default: 10)
   */
  int getEntriesPerPage(Player player);
  
  /**
   * Sets the number of entries to display per page for the given player.
   * @param player The player to set the preference for
   * @param entries The number of entries per page
   */
  void setEntriesPerPage(Player player, int entries);
  
  /**
   * Gets the default entries per page for all players.
   * @return The default number of entries per page
   */
  int getDefaultEntriesPerPage();
  
  /**
   * Gets whether the player prefers GUI mode over chat mode for info displays.
   * @param player The player to check
   * @return true if GUI mode is preferred, false for chat mode
   */
  boolean prefersGuiMode(Player player);
  
  /**
   * Sets whether the player prefers GUI mode over chat mode.
   * @param player The player to set the preference for
   * @param guiMode true for GUI mode, false for chat mode
   */
  void setGuiMode(Player player, boolean guiMode);
}
