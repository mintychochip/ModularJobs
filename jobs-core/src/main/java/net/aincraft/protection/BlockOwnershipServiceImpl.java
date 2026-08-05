package net.aincraft.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockOwnershipServiceImpl implements BlockOwnershipService {

  @Nullable
  private final BlockProtectionAdapter protectionAdapter;

  public BlockOwnershipServiceImpl(@org.jetbrains.annotations.Nullable BlockProtectionAdapter protectionAdapter) {
    this.protectionAdapter = protectionAdapter;
  }

  @Override
  public @NotNull Optional<OfflinePlayer> getOwner(Block block) {
    if (protectionAdapter == null) {
      return Optional.empty();
    }
    Optional<UUID> owner = protectionAdapter.getOwner(block);
    return owner.map(Bukkit::getOfflinePlayer);
  }
}
