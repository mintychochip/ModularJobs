package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.service.BuffService.BuffSlot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link MemoryBuffService} exclusive slot rules (P6 consumables). */
class MemoryBuffServiceTest {

  private static final UUID PLAYER = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

  private MutableClock clock;
  private MemoryBuffService buffs;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2026-06-01T12:00:00Z"));
    buffs = new MemoryBuffService(clock);
  }

  @Test
  void applyFoodBuff() {
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(10)));
    assertTrue(buffs.hasBuff(PLAYER, "stew"));
    assertEquals(1, buffs.activeBuffs(PLAYER).size());
  }

  @Test
  void foodAndPotionCanCoexist() {
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(10)));
    assertTrue(buffs.apply(PLAYER, "regen", BuffSlot.POTION, Duration.ofMinutes(5)));
    assertEquals(2, buffs.activeBuffs(PLAYER).size());
  }

  @Test
  void differentFoodInSameSlotRejected() {
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(10)));
    assertFalse(buffs.apply(PLAYER, "bread", BuffSlot.FOOD, Duration.ofMinutes(10)));
    assertTrue(buffs.hasBuff(PLAYER, "stew"));
    assertFalse(buffs.hasBuff(PLAYER, "bread"));
  }

  @Test
  void sameBuffRefreshesDuration() {
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(5)));
    clock.advance(Duration.ofMinutes(3));
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(10)));
    Instant expires = buffs.activeInSlot(PLAYER, BuffSlot.FOOD).orElseThrow().expiresAt();
    assertEquals(clock.instant().plus(Duration.ofMinutes(10)), expires);
  }

  @Test
  void expiredBuffFreesSlot() {
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(5)));
    clock.advance(Duration.ofMinutes(6));
    assertTrue(buffs.apply(PLAYER, "bread", BuffSlot.FOOD, Duration.ofMinutes(5)));
    assertTrue(buffs.hasBuff(PLAYER, "bread"));
    assertFalse(buffs.hasBuff(PLAYER, "stew"));
  }

  @Test
  void coatingIndependentSlot() {
    assertTrue(buffs.apply(PLAYER, "oil", BuffSlot.COATING, Duration.ofMinutes(3)));
    assertTrue(buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(3)));
    assertTrue(buffs.apply(PLAYER, "haste", BuffSlot.POTION, Duration.ofMinutes(3)));
    assertEquals(3, buffs.activeBuffs(PLAYER).size());
  }

  @Test
  void clearRemovesAll() {
    buffs.apply(PLAYER, "stew", BuffSlot.FOOD, Duration.ofMinutes(10));
    buffs.clear(PLAYER);
    assertTrue(buffs.activeBuffs(PLAYER).isEmpty());
  }

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
