package dev.mintychochip.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import dev.mintychochip.profession.RecipeDefinition;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Learned-recipe API for profession progression.
 */
public interface RecipeService {

  boolean knows(@NotNull UUID playerId, @NotNull Key recipeId);

  void grant(@NotNull UUID playerId, @NotNull Key recipeId);

  void revoke(@NotNull UUID playerId, @NotNull Key recipeId);

  @NotNull
  Set<Key> knownRecipes(@NotNull UUID playerId);

  void registerDefinition(@NotNull RecipeDefinition definition);

  /**
   * Returns every recipe definition registered at runtime (empty when none loaded).
   */
  default @NotNull Collection<RecipeDefinition> registeredDefinitions() {
    return Collections.emptyList();
  }
  Optional<RecipeDefinition> definition(@NotNull Key recipeId);



  /**
   * Resolves a registered recipe from the crafted item output key.
   *
   * <p>Default implementation matches {@link #definition(Key)} only; implementations may also
   * index {@link RecipeDefinition#craftOutputKey()}.
   */
  default Optional<RecipeDefinition> definitionForCraftOutput(@NotNull Key outputMaterialKey) {
    return definition(outputMaterialKey);
  }

  /**
   * Whether the player may craft this recipe.
   *
   * <ul>
   *   <li>If no {@link RecipeDefinition} is registered → allowed (vanilla BC).</li>
   *   <li>If registered → must know the recipe and meet required profession level.</li>
   * </ul>
   *
   * @param professionLevel current level in the recipe's profession (ignored if unregistered)
   */
  boolean canCraft(@NotNull UUID playerId, @NotNull Key recipeId, int professionLevel);
}
