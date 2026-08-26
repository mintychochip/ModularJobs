package dev.mintychochip.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * SPI for resolving the owning player of a protected block. Installed by {@link
 * BlockProtectionAdapterProvider} to bridge third-party protection plugins.
 */
@FunctionalInterface
@Internal
public interface BlockProtectionAdapter {

  /** Returns the owner. */
  Optional<UUID> getOwner(Block block);
}
