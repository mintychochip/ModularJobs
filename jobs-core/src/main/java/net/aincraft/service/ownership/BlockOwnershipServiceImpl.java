package net.aincraft.service.ownership;

import java.util.Optional;
import java.util.UUID;
import net.aincraft.service.BlockOwnershipService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class BlockOwnershipServiceImpl implements BlockOwnershipService {

  private final BlockProtectionAdapter protectionAdapter;

  public BlockOwnershipServiceImpl(BlockProtectionAdapter protectionAdapter) {
    this.protectionAdapter = protectionAdapter;
  }

  @Override
  public boolean isProtected(Block block) {
    return protectionAdapter.getOwner(block).isPresent();
  }

  @Override
  public @NotNull Optional<OfflinePlayer> getOwner(Block block) {
    Optional<UUID> owner = protectionAdapter.getOwner(block);
    return owner.map(Bukkit::getOfflinePlayer);
  }
}
