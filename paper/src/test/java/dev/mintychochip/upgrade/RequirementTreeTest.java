package dev.mintychochip.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import dev.mintychochip.upgrade.Requirements.AllOf;
import dev.mintychochip.upgrade.Requirements.AnyOf;
import dev.mintychochip.upgrade.Requirements.Not;
import dev.mintychochip.upgrade.Requirements.JobLevelRequirement;
import dev.mintychochip.upgrade.Requirements.NodeLevelRequirement;
import dev.mintychochip.upgrade.Requirements.NodeUnlockedRequirement;
import dev.mintychochip.upgrade.Requirements.PermissionRequirement;
import dev.mintychochip.upgrade.Requirements.StateEqualsRequirement;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class RequirementTreeTest {

  private SkillTreeState state() {
    return new SkillTreeState(
        "p1", "miner", 10,
        Map.of("efficiency", 2, "blasting", 1),
        Map.of(Key.key("tree", "vocation"), "weaponsmith"),
        () -> 25,
        key -> key.equals("jobs.special_access")
    );
  }

  @Test
  void nodeLevelLeaf() {
    assertTrue(new NodeLevelRequirement("efficiency", 2).satisfied(state()));
    assertTrue(new NodeLevelRequirement("efficiency", 1).satisfied(state()));
    assertFalse(new NodeLevelRequirement("efficiency", 3).satisfied(state()));
  }

  @Test
  void nodeUnlockedLeaf() {
    assertTrue(new NodeUnlockedRequirement("blasting").satisfied(state()));
    assertFalse(new NodeUnlockedRequirement("deep_mine").satisfied(state()));
  }

  @Test
  void jobLevelLeaf() {
    assertTrue(new JobLevelRequirement(25).satisfied(state()));
    assertFalse(new JobLevelRequirement(30).satisfied(state()));
  }

  @Test
  void stateEqualsLeaf() {
    assertTrue(new StateEqualsRequirement(Key.key("tree", "vocation"), "weaponsmith").satisfied(state()));
    assertFalse(new StateEqualsRequirement(Key.key("tree", "vocation"), "toolsmith").satisfied(state()));
  }

  @Test
  void permissionLeaf() {
    assertTrue(new PermissionRequirement("jobs.special_access").satisfied(state()));
    assertFalse(new PermissionRequirement("jobs.nope").satisfied(state()));
  }

  @Test
  void allOfRequiresEveryChild() {
    Requirement all = new AllOf(List.of(
        new NodeLevelRequirement("efficiency", 1),
        new NodeLevelRequirement("blasting", 1)
    ));
    assertTrue(all.satisfied(state()));

    Requirement allFail = new AllOf(List.of(
        new NodeLevelRequirement("efficiency", 1),
        new NodeLevelRequirement("deep_mine", 1)
    ));
    assertFalse(allFail.satisfied(state()));
  }

  @Test
  void anyOfRequiresOneChild() {
    Requirement any = new AnyOf(List.of(
        new JobLevelRequirement(30),
        new NodeLevelRequirement("efficiency", 2)
    ));
    assertTrue(any.satisfied(state()));

    Requirement anyFail = new AnyOf(List.of(
        new JobLevelRequirement(30),
        new NodeLevelRequirement("deep_mine", 1)
    ));
    assertFalse(anyFail.satisfied(state()));
  }

  @Test
  void notInverts() {
    assertTrue(new Not(new NodeUnlockedRequirement("deep_mine")).satisfied(state()));
    assertFalse(new Not(new NodeUnlockedRequirement("efficiency")).satisfied(state()));
  }
}
