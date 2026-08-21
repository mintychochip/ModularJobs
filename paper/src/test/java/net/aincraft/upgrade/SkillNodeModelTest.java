package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.Requirements.NodeLevelRequirement;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

/**
 * Proves the new node model carries levels, kind semantics, cumulative/replace
 * effect derivation, and precondition checks.
 */
class SkillNodeModelTest {

  private static SkillNode node(
      SkillNodeKind kind,
      int cost,
      List<NodeLevel> levels,
      LevelEffectMode mode,
      List<Requirement> requirements
  ) {
    return new SkillNode(
        Key.key("miner", "test"),
        "Test",
        "desc",
        "DIAMOND",
        "DIAMOND",
        null,
        null,
        kind,
        cost,
        levels.size(),
        mode,
        levels,
        requirements,
        Set.of(),
        Set.of(),
        List.of(),
        null,
        List.of(),
        List.of()
    );
  }

  @Test
  void kindSemantics() {
    SkillNode root = node(SkillNodeKind.ROOT, 0, List.of(), LevelEffectMode.REPLACE, List.of());
    final SkillNode skill = node(SkillNodeKind.SKILL, 0, List.of(new NodeLevel(1, List.of())), LevelEffectMode.REPLACE, List.of());
    final SkillNode major = node(SkillNodeKind.MAJOR, 5, List.of(), LevelEffectMode.REPLACE, List.of());

    assertTrue(root.isRoot());
    assertFalse(root.isSkill());
    assertFalse(root.isMajor());

    assertTrue(skill.isSkill());
    assertFalse(skill.isMajor());

    assertFalse(major.isSkill());
    assertTrue(major.isMajor());
  }

  @Test
  void levelCostOutOfRangeReturnsZero() {
    SkillNode skill = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())),
        LevelEffectMode.REPLACE,
        List.of()
    );
    assertEquals(1, skill.levelCost(1));
    assertEquals(2, skill.levelCost(2));
    assertEquals(0, skill.levelCost(0));
    assertEquals(0, skill.levelCost(3));
  }

  @Test
  void activeEffectsCumulativeVsReplace() {
    NodeEffect boost1 = NodeEffect.boost("xp", 1);
    NodeEffect boost2 = NodeEffect.boost("xp", 2);
    SkillNode cumulative = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of(boost1)), new NodeLevel(2, List.of(boost2))),
        LevelEffectMode.CUMULATIVE,
        List.of()
    );
    SkillNode replace = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of(boost1)), new NodeLevel(2, List.of(boost2))),
        LevelEffectMode.REPLACE,
        List.of()
    );

    // cumulative at level 2: level1 effect + level2 effect
    assertEquals(List.of(boost1, boost2), cumulative.activeEffects(2));
    // replace at level 2: only level2 effect
    assertEquals(List.of(boost2), replace.activeEffects(2));
    // at level 1 both behave the same
    assertEquals(List.of(boost1), cumulative.activeEffects(1));
    assertEquals(List.of(boost1), replace.activeEffects(1));
  }

  @Test
  void majorNeverHasLevels() {
    final SkillNode major = node(SkillNodeKind.MAJOR, 5, List.of(), LevelEffectMode.REPLACE, List.of());
    assertEquals(List.of(), major.activeEffects(0));
    assertEquals(List.of(), major.activeEffects(1));
  }

  @Test
  void preconditionSatisfiedGatesOnRequirements() {
    Requirement requirement = new NodeLevelRequirement("efficiency", 2);
    SkillNode nodeWithRequirement = node(
        SkillNodeKind.SKILL,
        0,
        List.of(new NodeLevel(1, List.of())),
        LevelEffectMode.REPLACE,
        List.of(requirement)
    );
    assertTrue(nodeWithRequirement.preconditionSatisfied(
        new SkillTreeState("player", "miner", 0, Map.of("efficiency", 2), Map.of())
    ));
    assertFalse(nodeWithRequirement.preconditionSatisfied(
        new SkillTreeState("player", "miner", 0, Map.of("efficiency", 1), Map.of())
    ));
  }
}
