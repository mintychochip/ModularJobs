package net.aincraft.service.ownership;

import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Provider;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.Protection;

public final class BoltBlockOwnershipProviderImpl implements Provider<Block, UUID> {

  private final BoltAPI bolt;

  public BoltBlockOwnershipProviderImpl(BoltAPI bolt) {
    this.bolt = bolt;
  }

  @Override
  public @NotNull Optional<UUID> get(Block key) {
    return Optional.of(bolt.findProtection(key).getOwner());
  }
}
