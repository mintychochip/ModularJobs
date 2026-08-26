package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.math.ExpressionCurves;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link JobProgressionImpl} level-from-experience binary search + XP thresholds.
 */
class JobProgressionLevelTest {

  private static final UUID PLAYER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  private Job job;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    // Threshold for level N is N * 100 XP (curve evaluates level variable)
    job =
        new JobImpl(
            Key.key("modularjobs", "miner"),
            Component.text("Miner"),
            Component.text("Mines blocks"),
            10,
            ExpressionCurves.levelingCurve("level * 100"),
            Map.of(),
            30,
            Map.of());
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void zeroExperienceIsLevelOne() {
    JobProgression progression = new JobProgressionImpl(PLAYER_ID, job, BigDecimal.ZERO);
    assertEquals(1, progression.level());
    assertEquals(0, BigDecimal.ZERO.compareTo(progression.experience()));
  }

  @Test
  void experienceAtLevelThresholds() {
    // curve: evaluate(level) = level * 100 → reaching level 3 requires >= 300 XP
    assertEquals(1, levelAt(new BigDecimal("99")));
    assertEquals(1, levelAt(new BigDecimal("100"))); // meets level-1 threshold only
    assertEquals(2, levelAt(new BigDecimal("200")));
    assertEquals(3, levelAt(new BigDecimal("300")));
    assertEquals(5, levelAt(new BigDecimal("500")));
    assertEquals(10, levelAt(new BigDecimal("1000")));
  }

  @Test
  void experienceAboveMaxLevelCapsAtMaxLevel() {
    assertEquals(10, levelAt(new BigDecimal("99999")));
  }

  @Test
  void experienceForLevelUsesJobCurve() {
    JobProgression progression = new JobProgressionImpl(PLAYER_ID, job, BigDecimal.ZERO);
    assertEquals(0, new BigDecimal("100.0").compareTo(progression.experienceForLevel(1)));
    assertEquals(0, new BigDecimal("500.0").compareTo(progression.experienceForLevel(5)));
    assertEquals(0, new BigDecimal("1000.0").compareTo(progression.experienceForLevel(10)));
  }

  @Test
  void setExperienceReturnsNewInstanceWithRecalculatedLevel() {
    JobProgression base = new JobProgressionImpl(PLAYER_ID, job, BigDecimal.ZERO);
    JobProgression leveled = base.setExperience(new BigDecimal("400"));
    assertNotSame(base, leveled);
    assertEquals(1, base.level());
    assertEquals(4, leveled.level());
    assertEquals(0, new BigDecimal("400").compareTo(leveled.experience()));
  }

  @Test
  void setExperienceSameValueReturnsSameInstance() {
    BigDecimal xp = new BigDecimal("250");
    JobProgression base = new JobProgressionImpl(PLAYER_ID, job, xp);
    assertSame(base, base.setExperience(xp));
  }

  @Test
  void addExperienceIncrementsAndLevelsUp() {
    JobProgression base = new JobProgressionImpl(PLAYER_ID, job, new BigDecimal("150"));
    JobProgression next = base.addExperience(new BigDecimal("100"));
    assertEquals(0, new BigDecimal("250").compareTo(next.experience()));
    assertTrue(next.level() >= base.level());
    assertEquals(2, next.level());
  }

  @Test
  void maxLevelZeroOrNegativeDefaultsToLevelOne() {
    Job uncapped =
        new JobImpl(
            Key.key("modularjobs", "free"),
            Component.text("Free"),
            Component.empty(),
            0,
            ExpressionCurves.levelingCurve("level * 100"),
            Map.of(),
            30,
            Map.of());
    JobProgression progression =
        new JobProgressionImpl(PLAYER_ID, uncapped, new BigDecimal("9999"));
    assertEquals(1, progression.level());
  }

  private int levelAt(BigDecimal experience) {
    return new JobProgressionImpl(PLAYER_ID, job, experience).level();
  }
}
