package dev.mintychochip.profession;

/**
 * Controls how crafting experience tapers when a player's profession level exceeds a recipe's
 * required level (MMO-style recipe depreciation).
 *
 * @param enabled when false, craft experience is never scaled
 * @param graceLevels full credit while {@code professionLevel <= requiredLevel + graceLevels}
 * @param windowLevels linear falloff from full credit to zero over this many levels above grace
 */
public record RecipeExperienceDepreciationPolicy(
    boolean enabled, int graceLevels, int windowLevels) {

  public static final int DEFAULT_GRACE_LEVELS = 0;
  public static final int DEFAULT_WINDOW_LEVELS = 10;

  public RecipeExperienceDepreciationPolicy {
    if (graceLevels < 0) {
      throw new IllegalArgumentException("graceLevels must be >= 0");
    }
    if (windowLevels < 0) {
      throw new IllegalArgumentException("windowLevels must be >= 0");
    }
  }

  /** Default policy: enabled with a ten-level linear taper after required level. */
  public static RecipeExperienceDepreciationPolicy defaults() {
    return new RecipeExperienceDepreciationPolicy(
        true, DEFAULT_GRACE_LEVELS, DEFAULT_WINDOW_LEVELS);
  }

  /** Explicitly disables recipe experience depreciation. */
  public static RecipeExperienceDepreciationPolicy disabled() {
    return new RecipeExperienceDepreciationPolicy(false, DEFAULT_GRACE_LEVELS, DEFAULT_WINDOW_LEVELS);
  }
}
