package dev.mintychochip.profession.content;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Registered recipe whose craft output has no paying craft job task anywhere. */
public record RegisteredRecipeWithoutTaskFinding(
    @NotNull Key recipeId,
    @NotNull Key craftOutputKey,
    @NotNull String professionId,
    int requiredLevel,
    @NotNull String message) {}
