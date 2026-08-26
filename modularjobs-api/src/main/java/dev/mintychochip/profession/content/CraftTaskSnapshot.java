package dev.mintychochip.profession.content;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Normalized craft job task for cross-validation (output key already resolved). */
public record CraftTaskSnapshot(
    @NotNull Key jobKey, @NotNull Key contextKey, @NotNull Key outputKey) {}
