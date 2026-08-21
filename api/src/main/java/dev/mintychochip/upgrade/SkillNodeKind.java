package dev.mintychochip.upgrade;

/** Determines how a node behaves in the skill graph. */
public enum SkillNodeKind {
  /** Starting point of a tree; normally cost 0, no requirements. */
  ROOT,
  /** Repeatable purchase, one per level, with per-level costs/effects. */
  SKILL,
  /** One-time permanent choice requiring player confirmation. */
  MAJOR
}
