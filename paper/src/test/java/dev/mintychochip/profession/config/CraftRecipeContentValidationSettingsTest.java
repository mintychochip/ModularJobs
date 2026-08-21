package dev.mintychochip.profession.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CraftRecipeContentValidationSettingsTest {

  @Test
  void defaultsWhenSectionMissing() {
    CraftRecipeContentValidationSettings.Settings settings =
        CraftRecipeContentValidationSettings.parse(new YamlConfiguration());

    assertTrue(settings.enabled());
    assertTrue(settings.warnTasksWithoutRecipe());
    assertTrue(settings.logRecipesWithoutTask());
    assertEquals(10, settings.maxDetailLines());
  }

  @Test
  void readsConfiguredValues() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("content-validation.craft-recipes.enabled", false);
    config.set("content-validation.craft-recipes.warn-tasks-without-recipe", false);
    config.set("content-validation.craft-recipes.log-recipes-without-task", true);
    config.set("content-validation.craft-recipes.max-detail-lines", 3);

    CraftRecipeContentValidationSettings.Settings settings =
        CraftRecipeContentValidationSettings.parse(config);

    assertFalse(settings.enabled());
    assertFalse(settings.warnTasksWithoutRecipe());
    assertTrue(settings.logRecipesWithoutTask());
    assertEquals(3, settings.maxDetailLines());
  }
}
