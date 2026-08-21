package dev.mintychochip.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A node in a job's skill graph. One model for all kinds: ROOT, SKILL, MAJOR.
 * Levelled skills carry per-level costs and effects; majors are one-time
 * permanent choices with optional state writes.
 */
public record SkillNode(
    @NotNull Key key,
    @NotNull String name,
    @Nullable String description,
    @NotNull String lockedIcon,
    @NotNull String unlockedIcon,
    @Nullable String lockedItemModel,
    @Nullable String unlockedItemModel,
    @NotNull SkillNodeKind kind,
    int cost,
    int maxLevel,
    @NotNull LevelEffectMode mode,
    @NotNull List<NodeLevel> levels,
    @NotNull List<Requirement> requirements,
    @NotNull Set<String> prerequisites,
    @NotNull Set<String> excludes,
    @NotNull List<NodeEffect> effects,
    @Nullable Position position,
    @NotNull List<Position> pathPoints,
    @NotNull List<NodeStateWrite> stateWrites
) implements Keyed {

  public enum LevelEffectMode {
    /** Active effects are effects of levels 1..current. */
    CUMULATIVE,
    /** Active effects are effects of the current level only. */
    REPLACE
  }

  public SkillNode {
    if (cost < 0 || maxLevel < 0) {
      throw new IllegalArgumentException("Skill node cost and maxLevel must be non-negative");
    }
    levels = List.copyOf(levels);
    requirements = List.copyOf(requirements);
    prerequisites = Set.copyOf(prerequisites);
    excludes = Set.copyOf(excludes);
    effects = List.copyOf(effects);
    pathPoints = List.copyOf(pathPoints);
    stateWrites = List.copyOf(stateWrites);
  }

  public boolean isRoot() {
    return kind == SkillNodeKind.ROOT;
  }

  public boolean isSkill() {
    return kind == SkillNodeKind.SKILL;
  }

  public boolean isMajor() {
    return kind == SkillNodeKind.MAJOR;
  }

  /** Cost to buy the given level (1-indexed); 0 when out of range. */
  public int levelCost(int level) {
    if (level < 1 || level > levels.size()) {
      return 0;
    }
    return levels.get(level - 1).cost();
  }

  /** Effects active at the given owned level, per {@link #mode}. */
  public @NotNull List<NodeEffect> activeEffects(int level) {
    if (level <= 0) {
      return List.of();
    }
    if (!isSkill()) {
      return effects;
    }
    int capped = Math.min(level, levels.size());
    if (mode == LevelEffectMode.CUMULATIVE) {
      List<NodeEffect> result = new ArrayList<>();
      for (int i = 1; i <= capped; i++) {
        result.addAll(levels.get(i - 1).effects());
      }
      return List.copyOf(result);
    }
    return levels.get(capped - 1).effects();
  }

  /**
   * Whether all configured requirements are satisfied for the given player state.
   */
  public @NotNull String getIconForState(boolean unlocked) {
    return unlocked ? unlockedIcon : lockedIcon;
  }

  @Nullable
  public String getItemModelForState(boolean unlocked) {
    return unlocked ? unlockedItemModel : lockedItemModel;
  }

  public boolean preconditionSatisfied(@NotNull SkillTreeState state) {
    for (Requirement requirement : requirements) {
      if (!requirement.satisfied(state)) {
        return false;
      }
    }
    return true;
  }
}
