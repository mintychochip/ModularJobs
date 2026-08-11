package net.aincraft.profession;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a learnable profession recipe.
 *
 * @param id               recipe key
 * @param professionId     canonical profession id (e.g. {@code weaponsmithing})
 * @param requiredLevel    minimum profession level to craft
 * @param tier             resource/recipe tier 1–5
 */
public record RecipeDefinition(
    @NotNull Key id,
    @NotNull String professionId,
    int requiredLevel,
    int tier
) {

  public RecipeDefinition {
    if (requiredLevel < 1) {
      throw new IllegalArgumentException("requiredLevel must be >= 1");
    }
    if (tier < 1 || tier > 5) {
      throw new IllegalArgumentException("tier must be 1–5");
    }
    professionId = professionId.toLowerCase();
  }
}
