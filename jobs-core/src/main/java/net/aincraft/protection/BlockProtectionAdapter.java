package net.aincraft.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
interface BlockProtectionAdapter {

  Optional<UUID> getOwner(Block block);
}
