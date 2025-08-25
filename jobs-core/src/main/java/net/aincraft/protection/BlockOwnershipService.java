package net.aincraft.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public interface BlockOwnershipService {
  @NotNull
  Optional<OfflinePlayer> getOwner(Block block);

}
