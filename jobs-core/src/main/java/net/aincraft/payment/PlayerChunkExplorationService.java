package net.aincraft.payment;

import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;

public interface PlayerChunkExplorationService {

  /**
   * Checks if the player has explored the given chunk.
   *
   * @param player the player to check
   * @param chunk  the chunk to check
   * @return true if explored, false otherwise
   */
  boolean hasExplored(OfflinePlayer player, Chunk chunk);

  /**
   * Marks the chunk as explored by the player.
   *
   * @param player the player exploring the chunk
   * @param chunk  the chunk explored
   */
  void addExploration(OfflinePlayer player, Chunk chunk);

  /**
   * Removes the player's exploration record for the chunk.
   *
   * @param player the player to remove
   * @param chunk  the chunk to clear
   */
  void removeExploration(OfflinePlayer player, Chunk chunk);
}
