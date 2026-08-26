package dev.mintychochip.upgrade;

import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Concrete requirement variants: logical combinators and typed leaves. */
public final class Requirements {

  private Requirements() {}

  /** Returns an all-of requirement over the given children. */
  public static Requirement allOf(@NotNull List<Requirement> requirements) {
    return new AllOf(requirements);
  }

  /** All children must be satisfied. */
  public record AllOf(@NotNull List<Requirement> requirements) implements Requirement {
    /** API member. */
    public AllOf {
      requirements = List.copyOf(requirements);
    }

    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return requirements.stream().allMatch(r -> r.satisfied(state));
    }
  }

  /** At least one child must be satisfied. */
  public record AnyOf(@NotNull List<Requirement> requirements) implements Requirement {
    /** API member. */
    public AnyOf {
      requirements = List.copyOf(requirements);
    }

    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return requirements.stream().anyMatch(r -> r.satisfied(state));
    }
  }

  /** Inverts the inner requirement. */
  public record Not(@NotNull Requirement requirement) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return !requirement.satisfied(state);
    }
  }

  /** Player's current level in this job must be at least {@code minimumJobLevel}. */
  public record JobLevelRequirement(int minimumJobLevel) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.jobLevel() >= minimumJobLevel;
    }
  }

  /** The named node must be owned at least at {@code minimum} level. */
  public record NodeLevelRequirement(@NotNull String nodeKey, int minimum) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.levelOf(nodeKey) >= minimum;
    }
  }

  /** The named node must be unlocked (level >= 1). */
  public record NodeUnlockedRequirement(@NotNull String nodeKey) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.hasUnlocked(nodeKey);
    }
  }

  /** A namespaced state key must equal {@code value}. */
  public record StateEqualsRequirement(@NotNull Key key, @NotNull String value)
      implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return value.equals(state.state().get(key));
    }
  }

  /** The player must have the given Bukkit permission. */
  public record PermissionRequirement(@NotNull String key) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.hasPermission(key);
    }
  }
}
