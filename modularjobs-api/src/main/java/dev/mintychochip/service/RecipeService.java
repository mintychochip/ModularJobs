package dev.mintychochip.service;

import dev.mintychochip.profession.RecipeDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Learned-recipe API for profession progression. */
public interface RecipeService {

  /** Knows. */
  boolean knows(@NotNull UUID playerId, @NotNull Key recipeId);

  /** Grant. */
  void grant(@NotNull UUID playerId, @NotNull Key recipeId);

  /** Revoke. */
  void revoke(@NotNull UUID playerId, @NotNull Key recipeId);

  /** API member. */
  @NotNull
  Set<Key> knownRecipes(@NotNull UUID playerId);

  /** Register definition. */
  void registerDefinition(@NotNull RecipeDefinition definition);

  /** Returns every recipe definition registered at runtime (empty when none loaded). */
  default @NotNull Collection<RecipeDefinition> registeredDefinitions() {
    return Collections.emptyList();
  }

  /** Definition. */
  Optional<RecipeDefinition> definition(@NotNull Key recipeId);

  /**
   * Resolves a registered recipe from the crafted item output key.
   *
   * <p>Default implementation matches {@link #definition(Key)} only; implementations may also index
   * {@link RecipeDefinition#craftOutputKey()}.
   */
  default Optional<RecipeDefinition> definitionForCraftOutput(@NotNull Key outputMaterialKey) {
    return definition(outputMaterialKey);
  }

  /**
   * Whether the player may craft this recipe.
   *
   * <ul>
   *   <li>If no {@link RecipeDefinition} is registered → allowed (vanilla BC).
   *   <li>If registered → must know the recipe and meet required profession level.
   * </ul>
   *
   * @param professionLevel current level in the recipe's profession (ignored if unregistered)
   */
  boolean canCraft(@NotNull UUID playerId, @NotNull Key recipeId, int professionLevel);
}
