package net.aincraft.service.ownership;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Provider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class LWCXBlockOwnershipProviderImpl implements Provider<Block,OfflinePlayer> {

  private final LWC lwc;

  public LWCXBlockOwnershipProviderImpl(LWC lwc) {
    this.lwc = lwc;
  }

  @Override
  public @NotNull Optional<OfflinePlayer> get(Block key) {
    if (!lwc.isProtectable(key)) {
      return Optional.empty();
    }
    Protection protection = lwc.findProtection(key);
    UUID owner = UUID.fromString(protection.getOwner());
    return Optional.of(Bukkit.getOfflinePlayer(owner));
  }
}
