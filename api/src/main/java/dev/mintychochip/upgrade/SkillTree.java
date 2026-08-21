package dev.mintychochip.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable skill graph for one base job. Children are derived from each
 * node's prerequisites; excludes are normalized to a symmetric conflict set.
 */
public record SkillTree(
    @NotNull Key key,
    @NotNull String jobKey,
    String description,
    int skillPointsPerLevel,
    @NotNull String rootNodeKey,
    @NotNull Map<String, SkillNode> nodeMap
) implements Keyed {

  public SkillTree {
    nodeMap = Collections.unmodifiableMap(new HashMap<>(nodeMap));
  }

  public @NotNull Optional<SkillNode> node(@NotNull String nodeKey) {
    return Optional.ofNullable(nodeMap.get(nodeKey));
  }


  public @NotNull Collection<SkillNode> nodes() {
    return nodeMap.values();
  }

  /** Children derived from prerequisites: nodes whose prerequisites include this node's key. */
  public @NotNull Collection<SkillNode> children(@NotNull SkillNode node) {
    String nodeKey = node.key().value();
    return nodeMap.values().stream()
        .filter(n -> n.prerequisites().contains(nodeKey))
        .collect(Collectors.toUnmodifiableList());
  }

  /** Node keys that conflict with {@code nodeKey} (both directions). */
  public @NotNull Set<String> symmetricExcludes(@NotNull String nodeKey) {
    Set<String> result = new HashSet<>();
    for (SkillNode node : nodeMap.values()) {
      String otherKey = node.key().value();
      if (node.excludes().contains(nodeKey)) {
        result.add(otherKey);
      }
      if (otherKey.equals(nodeKey)) {
        result.addAll(node.excludes());
      }
    }
    return Set.copyOf(result);
  }

  /** Points already spent: per-level costs plus one-time major costs. */
  public int spentPoints(@NotNull SkillTreeState state) {
    int spent = 0;
    for (Map.Entry<String, Integer> entry : state.nodeLevels().entrySet()) {
      SkillNode node = nodeMap.get(entry.getKey());
      if (node == null) {
        continue;
      }
      int owned = entry.getValue();
      if (node.isSkill()) {
        for (int level = 1; level <= owned; level++) {
          spent += node.levelCost(level);
        }
      } else if (node.isMajor() && owned >= 1) {
        spent += node.cost();
      }
    }
    return spent;
  }

  public int availablePoints(@NotNull SkillTreeState state) {
    return state.totalSkillPoints() - spentPoints(state);
  }

  /** Nodes a player can currently purchase (next skill level or whole major). */
  public @NotNull Set<SkillNode> availableNodes(@NotNull SkillTreeState state) {
    Set<SkillNode> result = new HashSet<>();
    for (SkillNode node : nodeMap.values()) {
      if (canPurchase(state, node.key().value())) {
        result.add(node);
      }
    }
    return Set.copyOf(result);
  }

  /** Full purchase gate: requirements, prereqs, excludes, ownership, and cost. */
  public boolean canPurchase(@NotNull SkillTreeState state, @NotNull String nodeKey) {
    SkillNode node = nodeMap.get(nodeKey);
    if (node == null) {
      return false;
    }

    // Requirements (configurable condition tree)
    if (!node.preconditionSatisfied(state)) {
      return false;
    }

    // Prerequisites owned
    for (String prereq : node.prerequisites()) {
      if (!state.hasUnlocked(prereq)) {
        return false;
      }
    }

    // Not excluded by an owned node
    for (String excluded : node.excludes()) {
      if (state.hasUnlocked(excluded)) {
        return false;
      }
    }
    for (Map.Entry<String, Integer> owned : state.nodeLevels().entrySet()) {
      if (owned.getValue() > 0 && symmetricExcludes(owned.getKey()).contains(nodeKey)) {
        return false;
      }
    }

    // Ownership level gating
    int owned = state.levelOf(nodeKey);
    if (node.isSkill()) {
      if (owned >= node.maxLevel()) {
        return false;
      }
    } else if (owned >= 1) {
      return false; // already owns this major or root
    }

    // Cost gate
    int cost = node.isSkill() ? node.levelCost(owned + 1) : node.cost();
    return cost <= availablePoints(state);
  }
}
