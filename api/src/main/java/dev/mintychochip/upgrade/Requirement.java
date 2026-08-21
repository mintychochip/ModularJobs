package dev.mintychochip.upgrade;

import org.jetbrains.annotations.NotNull;

/** A declarative condition in a skill tree. */
public sealed interface Requirement
    permits Requirements.AllOf,
        Requirements.AnyOf,
        Requirements.Not,
        Requirements.JobLevelRequirement,
        Requirements.NodeLevelRequirement,
        Requirements.NodeUnlockedRequirement,
        Requirements.StateEqualsRequirement,
        Requirements.PermissionRequirement {

  /** Satisfied. */
  boolean satisfied(@NotNull SkillTreeState state);
}
