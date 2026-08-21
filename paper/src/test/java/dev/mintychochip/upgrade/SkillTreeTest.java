package dev.mintychochip.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import dev.mintychochip.upgrade.Requirements.NodeLevelRequirement;
import dev.mintychochip.upgrade.SkillNode.LevelEffectMode;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class SkillTreeTest {

  private static SkillNode node(String jobKey, String nodeKey, SkillNodeKind kind,
                                int cost, List<NodeLevel> levels, List<Requirement> requirements,
                                Set<String> prerequisites, Set<String> excludes) {
    return new SkillNode(
        Key.key(jobKey, nodeKey), nodeKey, null,
        "DIAMOND", "DIAMOND", null, null,
        kind, cost, levels.size(), LevelEffectMode.REPLACE, levels, requirements,
        prerequisites, excludes, List.of(), null, List.of(), List.of());
  }

  private static SkillTree minerTree() {
    SkillNode root = node("miner", "root", SkillNodeKind.ROOT, 0, List.of(), List.of(), Set.of(), Set.of());
    SkillNode efficiency = node("miner", "efficiency", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())),
        List.of(), Set.of("root"), Set.of());
    SkillNode blasting = node("miner", "blasting", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(3, List.of())),
        List.of(), Set.of("efficiency"), Set.of("deep_mine"));
    SkillNode deepMine = node("miner", "deep_mine", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(3, List.of())),
        List.of(), Set.of("root"), Set.of("blasting"));
    SkillNode major = node("miner", "master_smith", SkillNodeKind.MAJOR, 5, List.of(), List.of(), Set.of("efficiency"), Set.of());

    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of("root", root, "efficiency", efficiency, "blasting", blasting,
            "deep_mine", deepMine, "master_smith", major));
  }

  @Test
  void childrenAreDerivedFromPrerequisites() {
    SkillTree tree = minerTree();
    assertEquals(
        Set.of("efficiency", "deep_mine"),
        tree.children(tree.node("root").orElseThrow()).stream()
            .map(n -> n.key().value()).collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        Set.of("blasting", "master_smith"),
        tree.children(tree.node("efficiency").orElseThrow()).stream()
            .map(n -> n.key().value()).collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void spentPointsSumsLevelsAndMajors() {
    SkillTree tree = minerTree();
    SkillTreeState state = new SkillTreeState(
        "p1", "miner", 15,
        Map.of("efficiency", 2, "blasting", 1, "master_smith", 1),
        Map.of(), () -> 5, k -> true);
    // efficiency level1 (1) + level2 (2) + blasting (3) + major (5) = 11
    assertEquals(11, tree.spentPoints(state));
    assertEquals(4, tree.availablePoints(state));
  }

  @Test
  void symmetricExcludesNormalized() {
    SkillTree tree = minerTree();
    assertTrue(tree.symmetricExcludes("blasting").contains("deep_mine"));
    assertTrue(tree.symmetricExcludes("deep_mine").contains("blasting"));
  }

  @Test
  void canPurchaseGatesOnRequirementsCostAndExcludes() {
    SkillTree tree = minerTree();
    SkillTreeState rich = new SkillTreeState(
        "p1", "miner", 20,
        Map.of("root", 1, "efficiency", 2),
        Map.of(), () -> 5, k -> true);

    // blasting: prereq efficiency owned, cost 3 <= available 17, not excluded
    assertTrue(tree.canPurchase(rich, "blasting"));
    // deep_mine is purchasable while blasting is not owned.
    assertTrue(tree.canPurchase(rich, "deep_mine"));

    SkillTreeState excluded = new SkillTreeState(
        "p1", "miner", 20,
        Map.of("root", 1, "efficiency", 2, "blasting", 1),
        Map.of(), () -> 5, k -> true);
    assertFalse(tree.canPurchase(excluded, "deep_mine"));
  }

  @Test
  void requirementsGateMajorPurchase() {
    SkillTree tree = minerTree();
    SkillNode master = tree.node("master_smith").orElseThrow();
    SkillNode withRequirement = new SkillNode(
        master.key(), master.name(), master.description(),
        master.lockedIcon(), master.unlockedIcon(),
        master.lockedItemModel(), master.unlockedItemModel(),
        master.kind(), master.cost(), master.maxLevel(), master.mode(),
        master.levels(),
        List.of(new NodeLevelRequirement("efficiency", 2)),
        master.prerequisites(), master.excludes(), master.effects(), master.position(),
        master.pathPoints(), master.stateWrites());

    SkillTree tree2 = new SkillTree(tree.key(), tree.jobKey(), tree.description(),
        tree.skillPointsPerLevel(), tree.rootNodeKey(),
        Map.of("root", tree.node("root").orElseThrow(),
            "efficiency", tree.node("efficiency").orElseThrow(),
            "master_smith", withRequirement));

    SkillTreeState lowEfficiency = new SkillTreeState(
        "p1", "miner", 20, Map.of("root", 1, "efficiency", 1),
        Map.of(), () -> 5, k -> true);
    assertFalse(tree2.canPurchase(lowEfficiency, "master_smith"));

    SkillTreeState highEfficiency = new SkillTreeState(
        "p1", "miner", 20, Map.of("root", 1, "efficiency", 2),
        Map.of(), () -> 5, k -> true);
    assertTrue(tree2.canPurchase(highEfficiency, "master_smith"));
  }
}
