package net.aincraft.service.ownership;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.Provider;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class LWCXBlockOwnershipProviderImpl implements Provider<Block,UUID> {

  private final LWC lwc;

  public LWCXBlockOwnershipProviderImpl(LWC lwc) {
    this.lwc = lwc;
  }

  @Override
  public @NotNull Optional<UUID> get(Block key) {
    if (!lwc.isProtectable(key)) {
      return Optional.empty();
    }
    Protection protection = lwc.findProtection(key);
    return Optional.of(UUID.fromString(protection.getOwner()));
  }
}
