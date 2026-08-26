package dev.mintychochip.upgrade;

import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Syncs a player's active effects to the DERIVED set of their skill tree state. Effects are never
 * one-time mutations: {@link #syncEffects} diffs previous and current states so a replace-level
 * downgrade revokes the old level's effects; {@link #restoreAllForTrees} clears all plugin-owned
 * contributions then applies the union of current sets (login/reload). No last-applied snapshot is
 * persisted or cached as player progression.
 */
public interface NodeEffectApplier {

  /** The full active effect set the given state implies (pure computation). */
  @NotNull
  Set<NodeEffect> derive(@NotNull SkillTreeState state, @NotNull SkillTree tree);

  /** Sync effects. */
  void syncEffects(
      @NotNull Player player,
      @NotNull SkillTreeState previous,
      @NotNull SkillTreeState current,
      @NotNull SkillTree tree);

  /**
   * Incremental mutation path across all active trees. Permission effects are diffed at individual
   * permission-string granularity, and the previous and current sets are each the union across the
   * given trees, so a permission still derived by any active tree is never revoked by a mutation in
   * another tree.
   */
  void syncEffects(
      @NotNull Player player,
      @NotNull Map<SkillTree, SkillTreeState> previousByTree,
      @NotNull Map<SkillTree, SkillTreeState> currentByTree);

  /** Restore all for trees. */
  void restoreAllForTrees(@NotNull Player player, @NotNull Map<SkillTree, SkillTreeState> byTree);

  /** Unapply all. */
  void unapplyAll(@NotNull Player player, @NotNull SkillTreeState state, @NotNull SkillTree tree);
}
