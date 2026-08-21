package dev.mintychochip.profession;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure recipe experience scaling: lower-tier recipes credit less (or no) profession experience once
 * the player outlevels them.
 */
public final class RecipeExperienceDepreciation {

  private static final int MULTIPLIER_SCALE = 4;

  private RecipeExperienceDepreciation() {}

  /**
   * Returns an experience multiplier in {@code [0, 1]} for crafting a recipe at the given
   * profession level.
   *
   * <p>Full credit while {@code professionLevel <= requiredLevel + graceLevels}. When {@code
   * windowLevels > 0}, credit linearly falls to zero over the next {@code windowLevels}. When
   * {@code windowLevels == 0}, credit drops to zero immediately after the grace band.
   */
  public static BigDecimal experienceMultiplier(
      RecipeExperienceDepreciationPolicy policy, int professionLevel, int recipeRequiredLevel) {
    if (policy == null || !policy.enabled()) {
      return BigDecimal.ONE;
    }
    int delta = professionLevel - recipeRequiredLevel - policy.graceLevels();
    if (delta <= 0) {
      return BigDecimal.ONE;
    }
    if (policy.windowLevels() == 0) {
      return BigDecimal.ZERO;
    }
    if (delta >= policy.windowLevels()) {
      return BigDecimal.ZERO;
    }
    BigDecimal remaining =
        BigDecimal.ONE.subtract(
            BigDecimal.valueOf(delta)
                .divide(
                    BigDecimal.valueOf(policy.windowLevels()),
                    MULTIPLIER_SCALE,
                    RoundingMode.HALF_UP));
    return remaining.max(BigDecimal.ZERO).min(BigDecimal.ONE);
  }
}
