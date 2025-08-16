package net.aincraft.service;

import net.aincraft.Bridge;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;

/**
 * Tracks which players have explored which chunks.
 *
 * <p>Use {@link #chunkExplorationStore()} to access the current implementation,
 * which may be backed by persistent data, a database, or other storage.
 */
public interface ChunkExplorationStore {

  /**
   * Returns the current {@link ChunkExplorationStore} instance via the global {@link Bridge}.
   */
  static ChunkExplorationStore chunkExplorationStore() {
    return Bridge.bridge().chunkExplorationStore();
  }

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
