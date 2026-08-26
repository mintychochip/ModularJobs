package dev.mintychochip.profession.content;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Normalized registered recipe metadata for cross-validation. */
public record RegisteredRecipeSnapshot(
    @NotNull Key recipeId,
    @NotNull Key craftOutputKey,
    @NotNull String professionId,
    int requiredLevel) {}
