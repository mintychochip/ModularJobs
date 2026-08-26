package dev.mintychochip.profession.content;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Result of comparing craft job tasks against registered recipe metadata. */
public record CraftRecipeValidationReport(
    @NotNull List<CraftTaskWithoutRecipeFinding> tasksWithoutRecipe,
    @NotNull List<RegisteredRecipeWithoutTaskFinding> recipesWithoutTask) {

  /** API member. */
  public CraftRecipeValidationReport {
    tasksWithoutRecipe = List.copyOf(tasksWithoutRecipe);
    recipesWithoutTask = List.copyOf(recipesWithoutTask);
  }
}
