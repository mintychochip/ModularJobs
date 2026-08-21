package dev.mintychochip.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressionLimitsConfigTest {

  @Test
  void parsesLimitsAndAutoJoin() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(java.util.Map.of(
        "max-jobs", 3,
        "auto-join-jobs", List.of("miner", "farmer"),
        "world-join-restriction", true));
    assertEquals(3, config.maxJobs());
    assertEquals(List.of("miner", "farmer"), config.autoJoinJobs());
    assertTrue(config.worldJoinRestriction());
  }

  @Test
  void defaults() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(java.util.Map.of());
    assertEquals(0, config.maxJobs()); // 0 = unlimited
    assertTrue(config.autoJoinJobs().isEmpty());
    assertTrue(config.worldJoinRestriction());
  }

  @Test
  void clampsNegativeMaxJobsAndNormalizesAutoJoinNames() {
    ProgressionLimitsConfig config = ProgressionLimitsConfig.fromMap(java.util.Map.of(
        "max-jobs", -5,
        "auto-join-jobs", List.of("Miner", "  ", "FARMER")));
    assertEquals(0, config.maxJobs());
    assertEquals(List.of("miner", "farmer"), config.autoJoinJobs());
  }
}
