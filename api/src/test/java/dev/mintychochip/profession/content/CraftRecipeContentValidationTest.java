package dev.mintychochip.profession.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class CraftRecipeContentValidationTest {

  @Test
  void matchedCraftTaskAndRecipeProduceNoFindings() {
    Key ironSword = Key.key("minecraft", "iron_sword");
    CraftRecipeValidationReport report = CraftRecipeContentValidation.validate(
        List.of(new CraftTaskSnapshot(
            Key.key("modularjobs", "blacksmith"),
            ironSword,
            ironSword)),
        List.of(new RegisteredRecipeSnapshot(
            ironSword,
            ironSword,
            "weaponsmithing",
            10)));

    assertTrue(report.tasksWithoutRecipe().isEmpty());
    assertTrue(report.recipesWithoutTask().isEmpty());
  }

  @Test
  void craftTaskWithoutRecipeMetadataIsReported() {
    Key stoneBricks = Key.key("minecraft", "stone_bricks");
    CraftRecipeValidationReport report = CraftRecipeContentValidation.validate(
        List.of(new CraftTaskSnapshot(
            Key.key("modularjobs", "artisan"),
            stoneBricks,
            stoneBricks)),
        List.of());

    assertEquals(1, report.tasksWithoutRecipe().size());
    assertTrue(report.tasksWithoutRecipe().get(0).message().contains("recipes.yml"));
    assertTrue(report.recipesWithoutTask().isEmpty());
  }

  @Test
  void registeredRecipeWithoutCraftTaskIsReported() {
    Key ironSword = Key.key("minecraft", "iron_sword");
    CraftRecipeValidationReport report = CraftRecipeContentValidation.validate(
        List.of(),
        List.of(new RegisteredRecipeSnapshot(
            ironSword,
            ironSword,
            "weaponsmithing",
            10)));

    assertTrue(report.tasksWithoutRecipe().isEmpty());
    assertEquals(1, report.recipesWithoutTask().size());
    assertTrue(report.recipesWithoutTask().get(0).message().contains("modularjobs:craft"));
  }

  @Test
  void outputKeyMatchUsesCraftOutputKeyNotRecipeId() {
    Key recipeId = Key.key("modularjobs", "masterwork_iron_sword");
    Key ironSword = Key.key("minecraft", "iron_sword");
    CraftRecipeValidationReport report = CraftRecipeContentValidation.validate(
        List.of(new CraftTaskSnapshot(
            Key.key("modularjobs", "blacksmith"),
            ironSword,
            ironSword)),
        List.of(new RegisteredRecipeSnapshot(
            recipeId,
            ironSword,
            "weaponsmithing",
            25)));

    assertTrue(report.tasksWithoutRecipe().isEmpty());
    assertTrue(report.recipesWithoutTask().isEmpty());
  }
}
