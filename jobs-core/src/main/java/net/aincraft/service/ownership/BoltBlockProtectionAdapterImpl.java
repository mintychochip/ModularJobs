package net.aincraft.service.ownership;

import java.util.Optional;
import java.util.UUID;
import net.aincraft.service.BlockOwnershipService.BlockProtectionAdapter;
import org.bukkit.block.Block;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.Protection;

public final class BoltBlockProtectionAdapterImpl implements BlockProtectionAdapter {

  private final BoltAPI bolt;

  public BoltBlockProtectionAdapterImpl(BoltAPI bolt) {
    this.bolt = bolt;
  }

  @Override
  public Optional<UUID> getOwner(Block block) {
    Protection protection = bolt.findProtection(block);
    return Optional.ofNullable(protection.getOwner());
  }
}
