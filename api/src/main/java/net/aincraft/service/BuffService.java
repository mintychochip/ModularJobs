package net.aincraft.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Consumable combat buff application rules (food / potion / coating slots).
 *
 * <p>This service tracks active buff ids and slots.
 */
public interface BuffService {

  enum BuffSlot {
    FOOD,
    POTION,
    COATING
  }

  record ActiveBuff(
      @NotNull String buffId,
      @NotNull BuffSlot slot,
      @NotNull Instant expiresAt
  ) {
    public boolean isExpired(Instant now) {
      return !expiresAt.isAfter(now);
    }
  }

  /**
   * Apply a buff. Fails if the slot is occupied by a different non-expired buff (no illegal stack).
   * Re-applying the same buff id refreshes duration.
   *
   * @return true if applied
   */
  boolean apply(
      @NotNull UUID playerId,
      @NotNull String buffId,
      @NotNull BuffSlot slot,
      @NotNull Duration duration);

  @NotNull
  List<ActiveBuff> activeBuffs(@NotNull UUID playerId);

  Optional<ActiveBuff> activeInSlot(@NotNull UUID playerId, @NotNull BuffSlot slot);

  boolean hasBuff(@NotNull UUID playerId, @NotNull String buffId);

  void clear(@NotNull UUID playerId);
}
