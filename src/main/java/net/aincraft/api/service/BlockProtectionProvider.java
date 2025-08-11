package net.aincraft.api.service;

import java.time.temporal.TemporalAmount;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

public interface BlockProtectionProvider {

  void removeProtection(@NotNull Block block);

  void addProtection(@NotNull Block block, @NotNull TemporalAmount temporalAmount);

  boolean isProtected(@NotNull Block block);

  /**
   * Returns the remaining duration for which the given block is protected.
   * <p>
   * If the block is not currently protected or its protection has expired,
   * this method returns {@link java.time.Duration#ZERO}.
   *
   * @param block the block to check
   * @return the remaining protection time, or {@code Duration.ZERO} if unprotected
   */
  @NotNull
  TemporalAmount getRemainingProtectionTime(@NotNull Block block);
}
