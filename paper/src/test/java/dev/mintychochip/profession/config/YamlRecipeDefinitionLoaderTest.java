package dev.mintychochip.profession.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.io.TempDir;
import dev.mintychochip.profession.MemoryRecipeService;
import dev.mintychochip.profession.RecipeDefinition;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlRecipeDefinitionLoaderTest {

  @Test
  void parsesStarterShapeWithDistinctOutputKey() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("recipes.modularjobs:masterwork_iron_sword.profession", "blacksmith");
    config.set("recipes.modularjobs:masterwork_iron_sword.required-level", 25);
    config.set("recipes.modularjobs:masterwork_iron_sword.tier", 3);
    config.set("recipes.modularjobs:masterwork_iron_sword.output", "minecraft:iron_sword");

    RecipeDefinition definition =
        YamlRecipeDefinitionLoader.parseDefinition(
            "modularjobs:masterwork_iron_sword",
            config.getConfigurationSection("recipes.modularjobs:masterwork_iron_sword"));

    assertEquals(Key.key("modularjobs", "masterwork_iron_sword"), definition.id());
    assertEquals("weaponsmithing", definition.professionId());
    assertEquals(25, definition.requiredLevel());
    assertEquals(3, definition.tier());
    assertEquals(Key.key("minecraft", "iron_sword"), definition.craftOutputKey());
  }

  @Test
  void loadFromConfigurationRegistersAllEntries() {
    MemoryRecipeService recipes = new MemoryRecipeService();
    YamlConfiguration config = new YamlConfiguration();
    config.set("recipes.minecraft:iron_sword.profession", "weaponsmithing");
    config.set("recipes.minecraft:iron_sword.required-level", 10);
    config.set("recipes.minecraft:iron_sword.tier", 2);

    int loaded =
        YamlRecipeDefinitionLoader.loadFromConfiguration(
            config, recipes, Logger.getGlobal());

    assertEquals(1, loaded);
    assertTrue(recipes.definition(Key.key("minecraft", "iron_sword")).isPresent());
    assertTrue(
        recipes
            .definitionForCraftOutput(Key.key("minecraft", "iron_sword"))
            .isPresent());
  }

  @Test
  void rejectsUnknownProfession() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("recipes.minecraft:iron_sword.profession", "not-a-profession");
    config.set("recipes.minecraft:iron_sword.required-level", 1);
    config.set("recipes.minecraft:iron_sword.tier", 1);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            YamlRecipeDefinitionLoader.parseAll(
                config.getConfigurationSection("recipes")));
  }

  @Test
  void emptySectionIsNoOp() {
    MemoryRecipeService recipes = new MemoryRecipeService();
    YamlConfiguration config = new YamlConfiguration();

    int loaded =
        YamlRecipeDefinitionLoader.loadFromConfiguration(
            config, recipes, Logger.getGlobal());

    assertEquals(0, loaded);
  }

  @Test
  void loadsBundledRecipesYmlWithoutDuplicateOutputConflict() {
    MemoryRecipeService recipes = new MemoryRecipeService();
    YamlConfiguration config;
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("recipes.yml")) {
      assertNotNull(in, "bundled recipes.yml must be on test classpath");
      config =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (Exception failure) {
      throw new AssertionError("failed to read bundled recipes.yml", failure);
    }

    int loaded =
        YamlRecipeDefinitionLoader.loadFromConfiguration(
            config, recipes, Logger.getGlobal());

    assertEquals(6, loaded);
    assertTrue(recipes.definitionForCraftOutput(Key.key("minecraft", "iron_sword")).isPresent());
    assertTrue(
        recipes.definitionForCraftOutput(Key.key("minecraft", "netherite_pickaxe")).isPresent());
    assertEquals(
        10,
        recipes
            .definitionForCraftOutput(Key.key("minecraft", "iron_pickaxe"))
            .orElseThrow()
            .requiredLevel());
  }

  @Test
  void bundledShapeRejectsDuplicateCraftOutputDuringParse() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("recipes.minecraft:iron_sword.profession", "weaponsmithing");
    config.set("recipes.minecraft:iron_sword.required-level", 10);
    config.set("recipes.minecraft:iron_sword.tier", 2);
    config.set("recipes.modularjobs:masterwork_iron_sword.profession", "weaponsmithing");
    config.set("recipes.modularjobs:masterwork_iron_sword.required-level", 25);
    config.set("recipes.modularjobs:masterwork_iron_sword.tier", 3);
    config.set("recipes.modularjobs:masterwork_iron_sword.output", "minecraft:iron_sword");

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlRecipeDefinitionLoader.loadFromConfiguration(
                    config, new MemoryRecipeService(), Logger.getGlobal()));
    assertTrue(failure.getMessage().contains("duplicate craft output"));
    assertTrue(failure.getMessage().contains("minecraft:iron_sword"));
  }

  @Test
  void loadFromDataFolderLoadsBundledResource(@TempDir Path dataFolder) throws Exception {
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("recipes.yml")) {
      assertNotNull(in, "bundled recipes.yml must be on test classpath");
      Files.copy(in, dataFolder.resolve("recipes.yml"));
    }

    MemoryRecipeService recipes = new MemoryRecipeService();
    int loaded =
        YamlRecipeDefinitionLoader.loadFromDataFolder(
            dataFolder.toFile(), recipes, Logger.getGlobal());

    assertEquals(6, loaded);
  }

  @Test
  void parseAllPreservesEntryOrder() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("recipes.minecraft:iron_sword.profession", "weaponsmithing");
    config.set("recipes.minecraft:iron_sword.required-level", 10);
    config.set("recipes.minecraft:iron_sword.tier", 2);
    config.set("recipes.minecraft:diamond_sword.profession", "weaponsmithing");
    config.set("recipes.minecraft:diamond_sword.required-level", 30);
    config.set("recipes.minecraft:diamond_sword.tier", 3);

    List<RecipeDefinition> parsed =
        YamlRecipeDefinitionLoader.parseAll(config.getConfigurationSection("recipes"));

    assertEquals(2, parsed.size());
  }
}
