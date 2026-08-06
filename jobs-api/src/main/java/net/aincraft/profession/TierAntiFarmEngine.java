package net.aincraft.profession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Pure tier / diminish / cooldown XP multiplier engine (AzothMC §8.2).
 *
 * <p>Call {@link #evaluateAndRecord} when awarding profession XP for a gather/craft/process action.
 * Multiplier is in {@code [0, 1]} and multiplies the base experience payable.
 */
public final class TierAntiFarmEngine {

  private final TierAntiFarmConfig config;
  private final Clock clock;
  private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

  public TierAntiFarmEngine(TierAntiFarmConfig config) {
    this(config, Clock.systemUTC());
  }

  public TierAntiFarmEngine(TierAntiFarmConfig config, Clock clock) {
    this.config = Objects.requireNonNull(config);
    this.clock = Objects.requireNonNull(clock);
  }

  /**
   * @param playerId     player receiving XP
   * @param actionKey    stable key for node/recipe/task (e.g. {@code mining|minecraft:iron_ore})
   * @param resourceTier recipe/node tier 1–5
   * @param playerLevel  current profession level
   * @return XP multiplier in {@code [0, 1]}
   */
  public double evaluateAndRecord(
      @NotNull UUID playerId,
      @NotNull String actionKey,
      int resourceTier,
      int playerLevel) {
    Instant now = clock.instant();
    String key = playerId + "|" + actionKey;
    Deque<Instant> history = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());

    prune(history, now);

    double multiplier = 1.0d;

    // Cooldown: no XP if last attempt was too recent
    Instant last = history.peekLast();
    if (last != null) {
      Duration sinceLast = Duration.between(last, now);
      if (sinceLast.compareTo(config.cooldown()) < 0) {
        history.addLast(now);
        return 0.0d;
      }
    }

    // Diminishing returns within window
    int prior = history.size();
    if (prior >= config.maxRepeatsBeforeZero()) {
      multiplier = 0.0d;
    } else if (prior > 0) {
      multiplier *= Math.pow(config.diminishFactor(), prior);
    }

    // Below-level mass processing penalty
    multiplier *= belowLevelFactor(resourceTier, playerLevel);

    history.addLast(now);
    return clamp01(multiplier);
  }

  /**
   * Peek multiplier without recording (for tests / UI).
   */
  public double peekMultiplier(
      @NotNull UUID playerId,
      @NotNull String actionKey,
      int resourceTier,
      int playerLevel) {
    Instant now = clock.instant();
    String key = playerId + "|" + actionKey;
    Deque<Instant> history = attemptsByKey.get(key);
    if (history == null || history.isEmpty()) {
      return clamp01(belowLevelFactor(resourceTier, playerLevel));
    }
    Deque<Instant> copy = new ArrayDeque<>(history);
    prune(copy, now);
    Instant last = copy.peekLast();
    if (last != null && Duration.between(last, now).compareTo(config.cooldown()) < 0) {
      return 0.0d;
    }
    int prior = copy.size();
    double multiplier = 1.0d;
    if (prior >= config.maxRepeatsBeforeZero()) {
      multiplier = 0.0d;
    } else if (prior > 0) {
      multiplier *= Math.pow(config.diminishFactor(), prior);
    }
    return clamp01(multiplier * belowLevelFactor(resourceTier, playerLevel));
  }

  public void clear() {
    attemptsByKey.clear();
  }

  public void clearPlayer(@NotNull UUID playerId) {
    String prefix = playerId + "|";
    attemptsByKey.keySet().removeIf(k -> k.startsWith(prefix));
  }

  private void prune(Deque<Instant> history, Instant now) {
    Instant cutoff = now.minus(config.window());
    while (!history.isEmpty() && history.peekFirst().isBefore(cutoff)) {
      history.removeFirst();
    }
  }

  private double belowLevelFactor(int resourceTier, int playerLevel) {
    int tier = Math.max(1, Math.min(5, resourceTier));
    int tierCeiling = tier * config.levelsPerTier();
    int penaltyStart = tierCeiling + config.belowLevelSlackLevels();
    if (playerLevel > penaltyStart) {
      return config.belowLevelXpFactor();
    }
    return 1.0d;
  }

  private static double clamp01(double v) {
    if (v < 0) {
      return 0;
    }
    if (v > 1) {
      return 1;
    }
    return v;
  }
}
