package net.aincraft.protection;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;

final class LWCXBlockProtectionAdapterImpl implements BlockProtectionAdapter {

  private final LWC lwc;

  LWCXBlockProtectionAdapterImpl(LWC lwc) {
    this.lwc = lwc;
  }

  @Override
  public Optional<UUID> getOwner(Block block) {
    Protection protection = lwc.findProtection(block);
    return Optional.of(UUID.fromString(protection.getOwner()));
  }
}
