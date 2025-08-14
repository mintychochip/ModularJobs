package net.aincraft.service.ownership;

import java.util.Optional;
import net.aincraft.api.container.Provider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.Protection;

public final class BoltBlockOwnershipProviderImpl implements Provider<Block,OfflinePlayer> {

  private final BoltAPI bolt;

  public BoltBlockOwnershipProviderImpl(BoltAPI bolt) {
    this.bolt = bolt;
  }

  @Override
  public @NotNull Optional<OfflinePlayer> get(Block key) {
    Protection protection = bolt.findProtection(key);
    return Optional.of(Bukkit.getOfflinePlayer(protection.getOwner()));
  }
}
