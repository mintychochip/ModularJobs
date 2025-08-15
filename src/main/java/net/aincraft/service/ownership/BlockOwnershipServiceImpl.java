package net.aincraft.service.ownership;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Provider;
import net.aincraft.api.service.BlockOwnershipService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class BlockOwnershipServiceImpl implements BlockOwnershipService {

  private final Provider<Block, UUID> blockOwnershipProvider;

  public BlockOwnershipServiceImpl(Provider<Block, UUID> blockOwnershipProvider) {
    this.blockOwnershipProvider = blockOwnershipProvider;
  }

  @Override
  public boolean isProtected(Block block) {
    return blockOwnershipProvider.get(block).isPresent();
  }

  @Override
  public @NotNull Optional<OfflinePlayer> getOwner(Block block) {
    Optional<UUID> ownerId = blockOwnershipProvider.get(block);
    Preconditions.checkArgument(ownerId.isPresent());
    return ownerId.map(Bukkit::getOfflinePlayer);
  }
}
