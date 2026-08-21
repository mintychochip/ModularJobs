package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link MemoryRecipeService} grant/know/gate rules (P6 recipe learning).
 */
class MemoryRecipeServiceTest {

  private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
  private static final Key RECIPE = Key.key("modularjobs", "iron_sword");

  private MemoryRecipeService recipes;

  @BeforeEach
  void setUp() {
    recipes = new MemoryRecipeService();
  }

  @Test
  void unregisteredRecipeCanAlwaysCraft() {
    assertTrue(recipes.canCraft(PLAYER, RECIPE, 1));
    assertFalse(recipes.knows(PLAYER, RECIPE));
  }

  @Test
  void registeredUnknownRecipeCannotCraft() {
    recipes.registerDefinition(new RecipeDefinition(RECIPE, "weaponsmithing", 5, 2));
    assertFalse(recipes.canCraft(PLAYER, RECIPE, 50));
  }

  @Test
  void grantThenCanCraftWhenLevelMet() {
    recipes.registerDefinition(new RecipeDefinition(RECIPE, "weaponsmithing", 5, 2));
    recipes.grant(PLAYER, RECIPE);
    assertTrue(recipes.knows(PLAYER, RECIPE));
    assertFalse(recipes.canCraft(PLAYER, RECIPE, 4), "below required level");
    assertTrue(recipes.canCraft(PLAYER, RECIPE, 5));
    assertTrue(recipes.canCraft(PLAYER, RECIPE, 20));
  }

  @Test
  void revokeRemovesKnowledge() {
    recipes.registerDefinition(new RecipeDefinition(RECIPE, "weaponsmithing", 1, 1));
    recipes.grant(PLAYER, RECIPE);
    recipes.revoke(PLAYER, RECIPE);
    assertFalse(recipes.knows(PLAYER, RECIPE));
    assertFalse(recipes.canCraft(PLAYER, RECIPE, 99));
  }

  @Test
  void knownRecipesListsGranted() {
    Key a = Key.key("modularjobs", "a");
    Key b = Key.key("modularjobs", "b");
    recipes.grant(PLAYER, a);
    recipes.grant(PLAYER, b);
    assertEquals(2, recipes.knownRecipes(PLAYER).size());
    assertTrue(recipes.knownRecipes(PLAYER).contains(a));
    assertTrue(recipes.knownRecipes(PLAYER).contains(b));
  }

  @Test
  void playersIsolated() {
    UUID other = UUID.fromString("99999999-9999-9999-9999-999999999999");
    recipes.registerDefinition(new RecipeDefinition(RECIPE, "alchemy", 1, 1));
    recipes.grant(PLAYER, RECIPE);
    assertFalse(recipes.knows(other, RECIPE));
    assertFalse(recipes.canCraft(other, RECIPE, 10));
  }
  @Test
  void definitionForCraftOutputResolvesDistinctOutputKey() {
    Key recipeId = Key.key("modularjobs", "masterwork_iron_sword");
    Key output = Key.key("minecraft", "iron_sword");
    recipes.registerDefinition(new RecipeDefinition(recipeId, "weaponsmithing", 5, 2, output));

    assertTrue(recipes.definitionForCraftOutput(output).isPresent());
    assertEquals(recipeId, recipes.definitionForCraftOutput(output).orElseThrow().id());
    assertTrue(recipes.definition(recipeId).isPresent());
    assertTrue(recipes.definitionForCraftOutput(Key.key("minecraft", "diamond")).isEmpty());
  }
  @Test
  void reRegisterWithChangedOutputKeyClearsStaleCraftOutputLookup() {
    Key recipeId = Key.key("modularjobs", "masterwork_iron_sword");
    Key oldOutput = Key.key("minecraft", "iron_sword");
    Key newOutput = Key.key("minecraft", "diamond_sword");

    recipes.registerDefinition(new RecipeDefinition(recipeId, "weaponsmithing", 5, 2, oldOutput));
    assertTrue(recipes.definitionForCraftOutput(oldOutput).isPresent());

    recipes.registerDefinition(new RecipeDefinition(recipeId, "weaponsmithing", 10, 3, newOutput));

    assertTrue(recipes.definitionForCraftOutput(newOutput).isPresent());
    assertEquals(10, recipes.definitionForCraftOutput(newOutput).orElseThrow().requiredLevel());
    assertTrue(recipes.definitionForCraftOutput(oldOutput).isEmpty());
    assertEquals(10, recipes.definition(recipeId).orElseThrow().requiredLevel());
  }

  @Test
  void duplicateCraftOutputKeyFromDifferentRecipesRejected() {
    Key recipeA = Key.key("modularjobs", "recipe_a");
    Key recipeB = Key.key("modularjobs", "recipe_b");
    Key output = Key.key("minecraft", "iron_sword");
    recipes.registerDefinition(new RecipeDefinition(recipeA, "weaponsmithing", 5, 2, output));

    IllegalArgumentException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> recipes.registerDefinition(
                new RecipeDefinition(recipeB, "weaponsmithing", 5, 2, output)));
    assertTrue(ex.getMessage().contains("iron_sword"));
  }

}
