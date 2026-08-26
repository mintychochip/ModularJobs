package dev.mintychochip.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the owning player of a {@link Block} through a pluggable {@link BlockProtectionAdapter}.
 * When no adapter is installed, every block is treated as unowned.
 */
public final class BlockOwnershipService {

  @Nullable private final BlockProtectionAdapter protectionAdapter;

  /** Creates the service delegating to the given adapter, which may be null. */
  public BlockOwnershipService(@Nullable BlockProtectionAdapter protectionAdapter) {
    this.protectionAdapter = protectionAdapter;
  }

  /**
   * Returns the owning {@link OfflinePlayer} of the block, or empty when no adapter is present or
   * the block has no recorded owner.
   */
  public @NotNull Optional<OfflinePlayer> getOwner(Block block) {
    if (protectionAdapter == null) {
      return Optional.empty();
    }
    Optional<UUID> owner = protectionAdapter.getOwner(block);
    return owner.map(Bukkit::getOfflinePlayer);
  }
}
