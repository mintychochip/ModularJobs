package dev.mintychochip.profession;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a learnable profession recipe.
 *
 * @param id recipe key
 * @param professionId canonical profession id (e.g. {@code weaponsmithing})
 * @param requiredLevel minimum profession level to craft
 * @param tier resource/recipe tier 1–5
 * @param outputKey crafted item key when it differs from {@code id}
 */
public record RecipeDefinition(
    @NotNull Key id,
    @NotNull String professionId,
    int requiredLevel,
    int tier,
    @NotNull Key outputKey) {

  /** Compact constructor validating level and tier. */
  public RecipeDefinition {
    if (requiredLevel < 1) {
      throw new IllegalArgumentException("requiredLevel must be >= 1");
    }
    if (tier < 1 || tier > 5) {
      throw new IllegalArgumentException("tier must be 1–5");
    }
    professionId = professionId.toLowerCase();
  }

  /** Backward-compatible constructor when the recipe id matches the crafted output. */
  public RecipeDefinition(
      @NotNull Key id, @NotNull String professionId, int requiredLevel, int tier) {
    this(id, professionId, requiredLevel, tier, id);
  }

  /** Key used to resolve this recipe from a crafted item stack. */
  public @NotNull Key craftOutputKey() {
    return outputKey;
  }
}
