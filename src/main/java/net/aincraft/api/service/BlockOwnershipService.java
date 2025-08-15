package net.aincraft.api.service;

import java.util.Optional;
import net.aincraft.api.Bridge;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public interface BlockOwnershipService {
  static Optional<BlockOwnershipService> blockOwnershipService() {
    return Bridge.bridge().blockOwnershipService();
  }
  boolean isProtected(Block block);

  @NotNull
  Optional<OfflinePlayer> getOwner(Block block);
}
