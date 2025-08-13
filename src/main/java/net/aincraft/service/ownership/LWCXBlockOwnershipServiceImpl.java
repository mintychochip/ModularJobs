package net.aincraft.service.ownership;

import com.google.common.base.Preconditions;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.UUID;
import net.aincraft.api.service.BlockOwnershipService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class LWCXBlockOwnershipServiceImpl implements BlockOwnershipService {

  private final LWC lwc;

  public LWCXBlockOwnershipServiceImpl(LWC lwc) {
    this.lwc = lwc;
  }

  @Override
  public boolean isProtected(@NotNull Block block) {
    if (!lwc.isProtectable(block)) {
      return false;
    }
    Protection protection = lwc.findProtection(block);
    return protection != null;
  }

  @Override
  public @NotNull OfflinePlayer getOwner(@NotNull Block block) throws IllegalArgumentException {
    Preconditions.checkArgument(isProtected(block));
    Protection protection = lwc.findProtection(block);
    UUID owner = UUID.fromString(protection.getOwner());
    return Bukkit.getOfflinePlayer(owner);
  }
}
