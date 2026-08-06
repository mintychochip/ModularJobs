package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link TierAntiFarmEngine} cooldown / diminish / below-level rules.
 */
class TierAntiFarmEngineTest {

  private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

  private MutableClock clock;
  private TierAntiFarmEngine engine;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    TierAntiFarmConfig config = new TierAntiFarmConfig(
        Duration.ofMinutes(5),
        Duration.ofSeconds(2),
        0.5d,
        4,
        0.1d,
        20,
        20
    );
    engine = new TierAntiFarmEngine(config, clock);
  }

  @Test
  void firstAttemptFullXp() {
    double m = engine.evaluateAndRecord(PLAYER, "mining|iron_ore", 2, 10);
    assertEquals(1.0d, m, 1e-9);
  }

  @Test
  void cooldownBlocksImmediateRepeat() {
    assertEquals(1.0d, engine.evaluateAndRecord(PLAYER, "node-a", 1, 5), 1e-9);
    clock.advance(Duration.ofMillis(500));
    assertEquals(0.0d, engine.evaluateAndRecord(PLAYER, "node-a", 1, 5), 1e-9);
  }

  @Test
  void afterCooldownDiminishesWithinWindow() {
    assertEquals(1.0d, engine.evaluateAndRecord(PLAYER, "node-b", 1, 5), 1e-9);
    clock.advance(Duration.ofSeconds(3));
    // prior=1 → 0.5
    assertEquals(0.5d, engine.evaluateAndRecord(PLAYER, "node-b", 1, 5), 1e-9);
    clock.advance(Duration.ofSeconds(3));
    // prior=2 → 0.25
    assertEquals(0.25d, engine.evaluateAndRecord(PLAYER, "node-b", 1, 5), 1e-9);
  }

  @Test
  void maxRepeatsYieldsZero() {
    for (int i = 0; i < 4; i++) {
      engine.evaluateAndRecord(PLAYER, "spam", 1, 5);
      clock.advance(Duration.ofSeconds(3));
    }
    // 4 prior attempts → maxRepeatsBeforeZero(4) → 0
    assertEquals(0.0d, engine.evaluateAndRecord(PLAYER, "spam", 1, 5), 1e-9);
  }

  @Test
  void belowLevelMassProcessPenalizes() {
    // T1 ceiling 20 + slack 20 = 40; level 50 triggers below-level factor 0.1
    double m = engine.evaluateAndRecord(PLAYER, "cheap-ore", 1, 50);
    assertEquals(0.1d, m, 1e-9);
  }

  @Test
  void highTierAtHighLevelNoPenalty() {
    // T3 ceiling 60 + slack 20 = 80; level 70 is fine
    double m = engine.evaluateAndRecord(PLAYER, "tier3", 3, 70);
    assertEquals(1.0d, m, 1e-9);
  }

  @Test
  void differentActionKeysIndependent() {
    engine.evaluateAndRecord(PLAYER, "a", 1, 5);
    clock.advance(Duration.ofMillis(100));
    assertEquals(1.0d, engine.evaluateAndRecord(PLAYER, "b", 1, 5), 1e-9);
  }

  @Test
  void windowExpiryResetsDiminish() {
    engine.evaluateAndRecord(PLAYER, "w", 1, 5);
    clock.advance(Duration.ofMinutes(6)); // past 5m window
    assertEquals(1.0d, engine.evaluateAndRecord(PLAYER, "w", 1, 5), 1e-9);
  }

  @Test
  void peekDoesNotRecord() {
    assertEquals(1.0d, engine.peekMultiplier(PLAYER, "p", 1, 5), 1e-9);
    assertEquals(1.0d, engine.peekMultiplier(PLAYER, "p", 1, 5), 1e-9);
    assertEquals(1.0d, engine.evaluateAndRecord(PLAYER, "p", 1, 5), 1e-9);
  }

  @Test
  void diminishAndBelowLevelStack() {
    engine.evaluateAndRecord(PLAYER, "stack", 1, 50); // first: 0.1 below-level
    clock.advance(Duration.ofSeconds(3));
    // prior=1 → 0.5 * 0.1 = 0.05
    assertEquals(0.05d, engine.evaluateAndRecord(PLAYER, "stack", 1, 50), 1e-9);
  }

  /** Mutable clock for deterministic anti-farm tests. */
  static final class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration d) {
      instant = instant.plus(d);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
