package net.aincraft.service;

import java.time.temporal.TemporalAmount;
import net.aincraft.database.ConnectionSource;
import net.aincraft.api.service.BlockProtectionProvider;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class DatabaseBlockProtectionProviderImpl implements BlockProtectionProvider {

  private final ConnectionSource connectionSource;

  public DatabaseBlockProtectionProviderImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public void removeProtection(@NotNull Block block) {

  }

  @Override
  public void addProtection(@NotNull Block block, @NotNull TemporalAmount temporalAmount) {

  }


  @Override
  public boolean isProtected(@NotNull Block block) {
    return false;
  }

  @Override
  public @NotNull TemporalAmount getRemainingProtectionTime(@NotNull Block block) {
    return null;
  }
}
