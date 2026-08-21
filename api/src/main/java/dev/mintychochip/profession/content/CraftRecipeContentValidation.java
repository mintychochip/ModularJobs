package dev.mintychochip.profession.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Pure cross-validation between craft job tasks and registered profession recipes.
 *
 * <p>Canonical sources remain separate: job tasks own pay; {@code recipes.yml} owns profession
 * gates and depreciation.
 */
public final class CraftRecipeContentValidation {

  private CraftRecipeContentValidation() {}

  /** Finds craft tasks without recipe metadata and registered recipes without craft pay tasks. */
  public static @NotNull CraftRecipeValidationReport validate(
      @NotNull List<CraftTaskSnapshot> craftTasks,
      @NotNull List<RegisteredRecipeSnapshot> recipes) {
    Set<Key> recipeOutputKeys = new HashSet<>();
    for (RegisteredRecipeSnapshot recipe : recipes) {
      recipeOutputKeys.add(recipe.craftOutputKey());
    }

    Set<Key> taskOutputKeys = new HashSet<>();
    for (CraftTaskSnapshot task : craftTasks) {
      taskOutputKeys.add(task.outputKey());
    }

    List<CraftTaskWithoutRecipeFinding> tasksWithoutRecipe = new ArrayList<>();
    for (CraftTaskSnapshot task : craftTasks) {
      if (recipeOutputKeys.contains(task.outputKey())) {
        continue;
      }
      tasksWithoutRecipe.add(
          new CraftTaskWithoutRecipeFinding(
              task.jobKey(),
              task.contextKey(),
              task.outputKey(),
              "Craft task "
                  + task.jobKey().asString()
                  + " / "
                  + task.contextKey().asString()
                  + " has no entry in recipes.yml for output "
                  + task.outputKey().asString()
                  + " — craft gate and recipe XP depreciation will not apply."));
    }

    List<RegisteredRecipeWithoutTaskFinding> recipesWithoutTask = new ArrayList<>();
    for (RegisteredRecipeSnapshot recipe : recipes) {
      if (taskOutputKeys.contains(recipe.craftOutputKey())) {
        continue;
      }
      recipesWithoutTask.add(
          new RegisteredRecipeWithoutTaskFinding(
              recipe.recipeId(),
              recipe.craftOutputKey(),
              recipe.professionId(),
              recipe.requiredLevel(),
              "Recipe "
                  + recipe.recipeId().asString()
                  + " ("
                  + recipe.professionId()
                  + " "
                  + recipe.requiredLevel()
                  + ") has no modularjobs:craft job task for output "
                  + recipe.craftOutputKey().asString()
                  + " — crafting will not pay job XP/money."));
    }

    return new CraftRecipeValidationReport(tasksWithoutRecipe, recipesWithoutTask);
  }
}
