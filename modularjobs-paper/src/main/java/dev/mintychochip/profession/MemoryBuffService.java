package dev.mintychochip.profession;

import dev.mintychochip.service.BuffService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/** In-memory consumable buff slots (food / potion / coating). No illegal cross-stacking. */
public final class MemoryBuffService implements BuffService {

  private final Clock clock;
  private final Map<UUID, Map<BuffSlot, ActiveBuff>> byPlayer = new ConcurrentHashMap<>();

  /** Memory buff service. */
  public MemoryBuffService() {
    this(Clock.systemUTC());
  }

  /** Memory buff service. */
  public MemoryBuffService(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public boolean apply(
      @NotNull UUID playerId,
      @NotNull String buffId,
      @NotNull BuffSlot slot,
      @NotNull Duration duration) {
    Instant now = clock.instant();
    Map<BuffSlot, ActiveBuff> slots =
        byPlayer.computeIfAbsent(playerId, id -> new EnumMap<>(BuffSlot.class));
    purgeExpired(slots, now);

    ActiveBuff existing = slots.get(slot);
    if (existing != null && !existing.isExpired(now)) {
      // Same buff id refreshes; different id in same slot is illegal stack
      if (!existing.buffId().equals(buffId)) {
        return false;
      }
    }

    Instant expires = now.plus(duration);
    slots.put(slot, new ActiveBuff(buffId, slot, expires));
    return true;
  }

  @Override
  public @NotNull List<ActiveBuff> activeBuffs(@NotNull UUID playerId) {
    Instant now = clock.instant();
    Map<BuffSlot, ActiveBuff> slots = byPlayer.get(playerId);
    if (slots == null || slots.isEmpty()) {
      return List.of();
    }
    purgeExpired(slots, now);
    return List.copyOf(slots.values());
  }

  @Override
  public Optional<ActiveBuff> activeInSlot(@NotNull UUID playerId, @NotNull BuffSlot slot) {
    Instant now = clock.instant();
    Map<BuffSlot, ActiveBuff> slots = byPlayer.get(playerId);
    if (slots == null) {
      return Optional.empty();
    }
    purgeExpired(slots, now);
    return Optional.ofNullable(slots.get(slot));
  }

  @Override
  public boolean hasBuff(@NotNull UUID playerId, @NotNull String buffId) {
    return activeBuffs(playerId).stream().anyMatch(b -> b.buffId().equals(buffId));
  }

  @Override
  public void clear(@NotNull UUID playerId) {
    byPlayer.remove(playerId);
  }

  private void purgeExpired(Map<BuffSlot, ActiveBuff> slots, Instant now) {
    List<BuffSlot> expired = new ArrayList<>();
    for (Map.Entry<BuffSlot, ActiveBuff> e : slots.entrySet()) {
      if (e.getValue().isExpired(now)) {
        expired.add(e.getKey());
      }
    }
    for (BuffSlot s : expired) {
      slots.remove(s);
    }
  }
}
