package net.aincraft.service.ownership;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.service.BlockOwnershipService.BlockProtectionAdapter;
import org.bukkit.block.Block;

public final class LWCXBlockProtectionAdapterImpl implements BlockProtectionAdapter {

  private final LWC lwc;

  public LWCXBlockProtectionAdapterImpl(LWC lwc) {
    this.lwc = lwc;
  }

  @Override
  public Optional<UUID> getOwner(Block block) {
    Protection protection = lwc.findProtection(block);
    return Optional.of(UUID.fromString(protection.getOwner()));
  }
}
