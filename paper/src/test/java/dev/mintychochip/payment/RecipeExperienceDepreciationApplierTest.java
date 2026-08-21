package dev.mintychochip.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.mintychochip.container.Context;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class RecipeExperienceDepreciationApplierTest {

  private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
  private static final Key RECIPE_ID = Key.key("modularjobs", "masterwork_iron_sword");
  private static final Key OUTPUT = Key.key("minecraft", "iron_sword");

  @Test
  void resolvesRegisteredRecipeByCraftOutputKey() {
    RecordingRecipeService recipes = new RecordingRecipeService();
    recipes.registerDefinition(new RecipeDefinition(RECIPE_ID, "weaponsmithing", 25, 2, OUTPUT));
    RecipeExperienceDepreciationApplier applier =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    BigDecimal scaled =
        applier.scaleCraftExperience(
            PLAYER, new Context.ItemContext("minecraft:iron_sword", 1), new BigDecimal("100"));

    assertEquals(OUTPUT, recipes.lastCraftOutputLookup());
    assertEquals(0, new BigDecimal("50").compareTo(scaled));
  }

  @Test
  void distinctRecipeIdWithoutOutputMappingDoesNotDepreciate() {
    RecordingRecipeService recipes = new RecordingRecipeService();
    recipes.registerDefinition(new RecipeDefinition(RECIPE_ID, "weaponsmithing", 25, 2));
    RecipeExperienceDepreciationApplier applier =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    BigDecimal amount = new BigDecimal("100");
    BigDecimal scaled =
        applier.scaleCraftExperience(
            PLAYER, new Context.ItemContext("minecraft:iron_sword", 1), amount);

    assertEquals(OUTPUT, recipes.lastCraftOutputLookup());
    assertEquals(0, amount.compareTo(scaled));
  }

  @Test
  void unregisteredRecipeIsUnchanged() {
    RecordingRecipeService recipes = new RecordingRecipeService();
    RecipeExperienceDepreciationApplier applier =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    BigDecimal amount = new BigDecimal("100");
    BigDecimal scaled =
        applier.scaleCraftExperience(
            PLAYER, new Context.ItemContext("minecraft:iron_sword", 1), amount);

    assertEquals(OUTPUT, recipes.lastCraftOutputLookup());
    assertNull(recipes.lastDefinitionLookup());
    assertEquals(0, amount.compareTo(scaled));
  }

  @Test
  void requestsExactCraftOutputKeyFromMaterialContext() {
    RecordingRecipeService recipes = new RecordingRecipeService();
    RecipeExperienceDepreciationApplier applier =
        new RecipeExperienceDepreciationApplier(
            new RecipeExperienceDepreciationPolicy(true, 0, 10),
            recipes,
            new RecordingProfessionService(30));

    applier.scaleCraftExperience(
        PLAYER, new Context.ItemContext("minecraft:iron_sword", 1), new BigDecimal("10"));

    assertEquals(OUTPUT, recipes.lastCraftOutputLookup());
  }

  private static final class RecordingRecipeService implements RecipeService {
    private final Map<Key, RecipeDefinition> byId = new HashMap<>();
    private final Map<Key, RecipeDefinition> byCraftOutput = new HashMap<>();
    private Key lastCraftOutputLookup;
    private Key lastDefinitionLookup;

    @Override
    public boolean knows(UUID playerId, Key recipeId) {
      return false;
    }

    @Override
    public void grant(UUID playerId, Key recipeId) {}

    @Override
    public void revoke(UUID playerId, Key recipeId) {}

    @Override
    public Set<Key> knownRecipes(UUID playerId) {
      return Set.of();
    }

    @Override
    public boolean canCraft(UUID playerId, Key recipeId, int professionLevel) {
      return false;
    }

    @Override
    public void registerDefinition(RecipeDefinition definition) {
      byId.put(definition.id(), definition);
      byCraftOutput.put(definition.craftOutputKey(), definition);
    }

    @Override
    public Optional<RecipeDefinition> definition(Key recipeId) {
      lastDefinitionLookup = recipeId;
      return Optional.ofNullable(byId.get(recipeId));
    }

    @Override
    public Optional<RecipeDefinition> definitionForCraftOutput(Key outputMaterialKey) {
      lastCraftOutputLookup = outputMaterialKey;
      return Optional.ofNullable(byCraftOutput.get(outputMaterialKey));
    }

    Key lastCraftOutputLookup() {
      return lastCraftOutputLookup;
    }

    Key lastDefinitionLookup() {
      return lastDefinitionLookup;
    }
  }

  private static final class RecordingProfessionService implements ProfessionService {
    private final int level;

    RecordingProfessionService(int level) {
      this.level = level;
    }

    @Override
    public java.util.List<dev.mintychochip.profession.ProfessionDefinition> tracks() {
      return java.util.List.of();
    }

    @Override
    public Optional<dev.mintychochip.profession.ProfessionDefinition> resolve(String idOrAlias) {
      return Optional.empty();
    }

    @Override
    public OptionalInt level(UUID playerId, String professionIdOrAlias) {
      return OptionalInt.of(level);
    }

    @Override
    public Optional<BigDecimal> experience(UUID playerId, String professionIdOrAlias) {
      return Optional.empty();
    }

    @Override
    public boolean ensureTrack(UUID playerId, String professionIdOrAlias) {
      return false;
    }
  }
}
