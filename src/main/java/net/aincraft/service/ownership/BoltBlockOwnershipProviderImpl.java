package net.aincraft.service.ownership;

import com.google.common.base.Preconditions;
import java.util.UUID;
import net.aincraft.api.service.BlockOwnershipProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.BlockProtection;

public final class BoltBlockOwnershipProviderImpl implements BlockOwnershipProvider {

  private final BoltAPI bolt;

  public BoltBlockOwnershipProviderImpl(BoltAPI bolt) {
    this.bolt = bolt;
  }

  @Override
  public boolean isProtected(@NotNull Block block) {
    if (!bolt.isProtectable(block)) {
      return false;
    }
    return bolt.isProtected(block);
  }

  @Override
  public @NotNull OfflinePlayer getOwner(@NotNull Block block) throws IllegalArgumentException {
    Preconditions.checkArgument(isProtected(block));
    BlockProtection protection = bolt.loadProtection(block);
    UUID owner = protection.getOwner();
    return Bukkit.getOfflinePlayer(owner);
  }
}
