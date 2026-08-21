package dev.mintychochip.profession;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.mintychochip.service.RecipeService;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory learned-recipe store.
 */
public final class MemoryRecipeService implements RecipeService {

  private final Map<UUID, Set<Key>> known = new ConcurrentHashMap<>();
  private final Map<Key, RecipeDefinition> definitions = new ConcurrentHashMap<>();
  private final Map<Key, RecipeDefinition> definitionsByCraftOutput = new ConcurrentHashMap<>();

  @Override
  public boolean knows(@NotNull UUID playerId, @NotNull Key recipeId) {
    Set<Key> set = known.get(playerId);
    return set != null && set.contains(recipeId);
  }

  @Override
  public void grant(@NotNull UUID playerId, @NotNull Key recipeId) {
    known.computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet()).add(recipeId);
  }

  @Override
  public void revoke(@NotNull UUID playerId, @NotNull Key recipeId) {
    Set<Key> set = known.get(playerId);
    if (set != null) {
      set.remove(recipeId);
    }
  }

  @Override
  public @NotNull Set<Key> knownRecipes(@NotNull UUID playerId) {
    Set<Key> set = known.get(playerId);
    if (set == null || set.isEmpty()) {
      return Set.of();
    }
    return Collections.unmodifiableSet(Set.copyOf(set));
  }

  @Override
  public void registerDefinition(@NotNull RecipeDefinition definition) {
    RecipeDefinition previous = definitions.get(definition.id());
    if (previous != null && !previous.craftOutputKey().equals(definition.craftOutputKey())) {
      RecipeDefinition mappedAtOldOutput = definitionsByCraftOutput.get(previous.craftOutputKey());
      if (mappedAtOldOutput != null && mappedAtOldOutput.id().equals(previous.id())) {
        definitionsByCraftOutput.remove(previous.craftOutputKey());
      }
    }

    RecipeDefinition existingForOutput = definitionsByCraftOutput.get(definition.craftOutputKey());
    if (existingForOutput != null && !existingForOutput.id().equals(definition.id())) {
      throw new IllegalArgumentException(
          "craft output key already registered: " + definition.craftOutputKey());
    }

    definitions.put(definition.id(), definition);
    definitionsByCraftOutput.put(definition.craftOutputKey(), definition);
  }

  @Override
  public Optional<RecipeDefinition> definition(@NotNull Key recipeId) {
    return Optional.ofNullable(definitions.get(recipeId));
  }
  @Override
  public @NotNull Collection<RecipeDefinition> registeredDefinitions() {
    if (definitions.isEmpty()) {
      return List.of();
    }
    return Collections.unmodifiableCollection(definitions.values());
  }


  @Override
  public Optional<RecipeDefinition> definitionForCraftOutput(@NotNull Key outputMaterialKey) {
    RecipeDefinition byOutput = definitionsByCraftOutput.get(outputMaterialKey);
    if (byOutput != null) {
      return Optional.of(byOutput);
    }
    return definition(outputMaterialKey);
  }

  @Override
  public boolean canCraft(@NotNull UUID playerId, @NotNull Key recipeId, int professionLevel) {
    RecipeDefinition def = definitions.get(recipeId);
    if (def == null) {
      // Unregistered recipes: vanilla BC — do not block
      return true;
    }
    if (!knows(playerId, recipeId)) {
      return false;
    }
    return professionLevel >= def.requiredLevel();
  }
}
