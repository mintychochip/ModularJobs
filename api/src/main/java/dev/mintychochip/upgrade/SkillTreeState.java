package dev.mintychochip.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of a player's progress in one job's skill tree.
 * Only {@link #totalSkillPoints()} and {@link #nodeLevels()} are persisted;
 * the state map is a derived view. Job-level and permission suppliers are
 * runtime evaluation context and are never persisted.
 */
public record SkillTreeState(
    @NotNull String playerId,
    @NotNull String jobKey,
    int totalSkillPoints,
    @NotNull Map<String, Integer> nodeLevels,
    @NotNull Map<Key, String> state,
    @NotNull IntSupplier currentJobLevel,
    @NotNull Predicate<String> permissionCheck
) {
  public SkillTreeState {
    nodeLevels = Collections.unmodifiableMap(new HashMap<>(nodeLevels));
    state = Collections.unmodifiableMap(new HashMap<>(state));
  }

  public SkillTreeState(
      String playerId,
      String jobKey,
      int totalSkillPoints,
      Map<String, Integer> nodeLevels,
      Map<Key, String> state) {
    this(playerId, jobKey, totalSkillPoints, nodeLevels, state, () -> 0, k -> false);
  }

  public int levelOf(@NotNull String nodeKey) {
    return nodeLevels.getOrDefault(nodeKey, 0);
  }

  public boolean hasUnlocked(@NotNull String nodeKey) {
    return levelOf(nodeKey) > 0;
  }

  public int jobLevel() {
    return currentJobLevel.getAsInt();
  }

  public boolean hasPermission(@NotNull String permission) {
    return permissionCheck.test(permission);
  }

  /** Spent points = sum of purchased level costs plus major costs. Needs the tree. */
  public static SkillTreeState empty(@NotNull String playerId, @NotNull String jobKey) {
    return new SkillTreeState(playerId, jobKey, 0, Map.of(), Map.of());
  }
}
