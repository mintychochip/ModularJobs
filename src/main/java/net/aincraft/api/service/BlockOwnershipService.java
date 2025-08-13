package net.aincraft.api.service;

import net.aincraft.api.Bridge;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to block ownership and protection data.
 * <p>
 * Implementations may integrate with plugins like LWC, Bolt, or others.
 */
public interface BlockOwnershipService {

  /**
   * Returns the active block ownership service, or {@code null} if none is registered.
   */
  static BlockOwnershipService blockOwnershipService() {
    return Bridge.bridge().blockOwnershipService();
  }

  /**
   * Checks if the given block is protected.
   *
   * @param block the block to check
   * @return true if protected
   */
  boolean isProtected(@NotNull Block block);

  /**
   * Gets the owner of a protected block.
   *
   * @param block the block
   * @return the owning player
   * @throws IllegalArgumentException if the block is not protected
   */
  @NotNull
  OfflinePlayer getOwner(@NotNull Block block) throws IllegalArgumentException;
}
