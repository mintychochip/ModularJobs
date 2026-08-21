package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecipeExperienceDepreciationTest {

  private static final RecipeExperienceDepreciationPolicy POLICY =
      new RecipeExperienceDepreciationPolicy(true, 0, 10);

  @Test
  void fullCreditAtRequiredLevel() {
    assertEquals(
        0,
        BigDecimal.ONE.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(POLICY, 25, 25)));
  }

  @Test
  void fullCreditBelowRequiredLevel() {
    assertEquals(
        0,
        BigDecimal.ONE.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(POLICY, 20, 25)));
  }

  @Test
  void linearHalfwayThroughWindow() {
    assertEquals(
        0,
        new BigDecimal("0.5").compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(POLICY, 30, 25)));
  }

  @Test
  void zeroCreditAfterWindow() {
    assertEquals(
        0,
        BigDecimal.ZERO.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(POLICY, 40, 25)));
  }

  @Test
  void graceLevelsDelayDepreciation() {
    RecipeExperienceDepreciationPolicy grace = new RecipeExperienceDepreciationPolicy(true, 2, 10);
    assertEquals(
        0,
        BigDecimal.ONE.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(grace, 27, 25)));
    assertEquals(
        0,
        new BigDecimal("0.9").compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(grace, 28, 25)));
  }

  @Test
  void zeroWindowIsHardCutoffAfterGrace() {
    RecipeExperienceDepreciationPolicy hard = new RecipeExperienceDepreciationPolicy(true, 0, 0);
    assertEquals(
        0,
        BigDecimal.ZERO.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(hard, 26, 25)));
  }

  @Test
  void disabledPolicyAlwaysFullCredit() {
    assertEquals(
        0,
        BigDecimal.ONE.compareTo(
            RecipeExperienceDepreciation.experienceMultiplier(
                RecipeExperienceDepreciationPolicy.disabled(), 99, 1)));
  }
}
