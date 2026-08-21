package dev.mintychochip.payment;

import dev.mintychochip.container.Context;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.profession.RecipeExperienceDepreciation;
import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Scales craft experience payables when a registered recipe is below the player's profession level.
 */
public final class RecipeExperienceDepreciationApplier {

  private final RecipeExperienceDepreciationPolicy policy;
  private final RecipeService recipeService;
  private final ProfessionService professionService;

  /** Recipe experience depreciation applier. */
  public RecipeExperienceDepreciationApplier(
      @NotNull RecipeExperienceDepreciationPolicy policy,
      @NotNull RecipeService recipeService,
      @NotNull ProfessionService professionService) {
    this.policy = policy;
    this.recipeService = recipeService;
    this.professionService = professionService;
  }

  /**
   * Applies recipe depreciation to a craft experience amount, or returns the original amount when
   * depreciation does not apply.
   */
  public @NotNull BigDecimal scaleCraftExperience(
      @NotNull UUID playerId, @NotNull Context context, @NotNull BigDecimal amount) {
    if (!policy.enabled() || amount.signum() == 0) {
      return amount;
    }
    if (!(context instanceof Context.ItemContext item)) {
      return amount;
    }
    Key outputKey = CraftRecipeLookup.outputKeyFromMaterialKey(item.materialKey());
    Optional<RecipeDefinition> definition =
        CraftRecipeLookup.definitionForCraftOutput(recipeService, outputKey);
    if (definition.isEmpty()) {
      return amount;
    }
    RecipeDefinition recipe = definition.get();
    int professionLevel = professionService.level(playerId, recipe.professionId()).orElse(0);
    BigDecimal multiplier =
        RecipeExperienceDepreciation.experienceMultiplier(
            policy, professionLevel, recipe.requiredLevel());
    return amount.multiply(multiplier);
  }
}
