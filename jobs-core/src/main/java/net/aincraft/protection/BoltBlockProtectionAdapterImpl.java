package net.aincraft.protection;

import java.util.Optional;
import java.util.UUID;
import net.aincraft.protection.BlockOwnershipService.BlockProtectionAdapter;
import org.bukkit.block.Block;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.Protection;

final class BoltBlockProtectionAdapterImpl implements BlockProtectionAdapter {

  private final BoltAPI bolt;

  BoltBlockProtectionAdapterImpl(BoltAPI bolt) {
    this.bolt = bolt;
  }

  @Override
  public Optional<UUID> getOwner(Block block) {
    Protection protection = bolt.findProtection(block);
    return Optional.ofNullable(protection.getOwner());
  }
}
