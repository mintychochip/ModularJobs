package dev.mintychochip.profession.content;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Craft job task whose output has no matching {@code recipes.yml} entry. */
public record CraftTaskWithoutRecipeFinding(
    @NotNull Key jobKey,
    @NotNull Key contextKey,
    @NotNull Key outputKey,
    @NotNull String message) {}
