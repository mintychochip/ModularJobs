package dev.mintychochip.profession;

import org.jetbrains.annotations.NotNull;

/**
 * One built-in profession track.
 *
 * @param id canonical id (e.g. {@code mining})
 * @param storageKey ModularJobs job key used for progression/tasks (e.g. {@code miner})
 * @param category gathering / processing / crafting
 * @param displayName plain English label
 */
public record ProfessionDefinition(
    @NotNull String id,
    @NotNull String storageKey,
    @NotNull ProfessionCategory category,
    @NotNull String displayName) {

  /** API member. */
  public ProfessionDefinition {
    id = id.toLowerCase();
    storageKey = storageKey.toLowerCase();
  }
}
