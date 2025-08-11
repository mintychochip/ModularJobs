package net.aincraft.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.api.service.BlockProtectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class InMemoryBlockProtectionProviderImpl implements BlockProtectionProvider {

  private final Map<Block, Instant> blockProtectionTimers = new HashMap<>();

  @Override
  public void removeProtection(@NotNull Block block) {
    blockProtectionTimers.remove(block);
  }

  @Override
  public void addProtection(@NotNull Block block, @NotNull TemporalAmount temporalAmount) {
    Instant expiryTime = Instant.now().plus(temporalAmount);
    blockProtectionTimers.put(block, expiryTime);
    Bukkit.broadcastMessage(blockProtectionTimers.toString());
  }

  @Override
  public boolean isProtected(@NotNull Block block) {
    if (!blockProtectionTimers.containsKey(block)) {
      return false;
    }
    @NotNull Instant expiryTime = blockProtectionTimers.get(block);
    if (expiryTime.isBefore(Instant.now())) {
      blockProtectionTimers.remove(block);
      return false;
    }
    return true;
  }

  @Override
  public @NotNull TemporalAmount getRemainingProtectionTime(@NotNull Block block) {
    Instant expiryTime = blockProtectionTimers.get(block);
    if (expiryTime == null) {
      return Duration.ZERO;
    }
    return Duration.between(Instant.now(), expiryTime);
  }
}
