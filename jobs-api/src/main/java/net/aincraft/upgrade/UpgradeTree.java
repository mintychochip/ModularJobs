package net.aincraft.upgrade;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a complete upgrade tree for a job.
 * Contains all nodes and provides traversal/query methods.
 */
public final class UpgradeTree implements Keyed {

  private final Key key;
  private final String jobKey;
  private final String description;
  private final String rootNodeKey;
  private final int skillPointsPerLevel;
  private final Map<String, UpgradeNode> nodes;
  private final Map<String, PerkPolicy> perkPolicies;
  private final Set<Position> paths;

  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @Nullable String description,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes,
      @NotNull Map<String, PerkPolicy> perkPolicies,
      @NotNull Set<Position> paths
  ) {
    this.key = key;
    this.jobKey = jobKey;
    this.description = description;
    this.rootNodeKey = rootNodeKey;
    this.skillPointsPerLevel = skillPointsPerLevel;
    this.nodes = new HashMap<>(nodes);
    this.perkPolicies = new HashMap<>(perkPolicies);
    this.paths = Set.copyOf(paths);
  }

  /**
   * Constructor for backward compatibility (no description, no perk policies).
   */
  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes
  ) {
    this(key, jobKey, null, rootNodeKey, skillPointsPerLevel, nodes, new HashMap<>(), Set.of());
  }

  /**
   * Constructor for backward compatibility (no perk policies).
   */
  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @Nullable String description,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes
  ) {
    this(key, jobKey, description, rootNodeKey, skillPointsPerLevel, nodes, new HashMap<>(), Set.of());
  }

  @Override
  public @NotNull Key key() {
    return key;
  }

  /**
   * The job this upgrade tree belongs to.
   */
  public @NotNull String jobKey() {
    return jobKey;
  }

  /**
   * The description of this upgrade tree.
   */
  public @Nullable String description() {
    return description;
  }

  /**
   * The root node key where the tree starts.
   */
  public @NotNull String rootNodeKey() {
    return rootNodeKey;
  }

  /**
   * How many skill points are earned per level-up.
   */
  public int skillPointsPerLevel() {
    return skillPointsPerLevel;
  }

  /**
   * Get all perk policies for this tree.
   * Maps perkId -> policy for how multiple levels are applied.
   */
  @NotNull
  public Map<String, PerkPolicy> perkPolicies() {
    return Collections.unmodifiableMap(perkPolicies);
  }

  /**
   * Get the policy for a specific perk.
   * @return the policy, or MAX if not specified (default behavior)
   */
  @NotNull
  public PerkPolicy getPerkPolicy(@NotNull String perkId) {
    return perkPolicies.getOrDefault(perkId, PerkPolicy.MAX);
  }

  /**
   * Get the maximum level available for a perk in this tree.
   * Scans all nodes to find the highest level for nodes with the given perkId.
   *
   * @param perkId the perk identifier (e.g., "far_gather", "crit_chance")
   * @return the highest level node for this perk, or 0 if not found
   */
  public int getMaxPerkLevel(@NotNull String perkId) {
    return nodes.values().stream()
        .filter(n -> perkId.equals(n.perkId()))
        .mapToInt(UpgradeNode::level)
        .max()
        .orElse(0);
  }

  /**
   * Get all path coordinates for this tree.
   * These are the walkable connection points between nodes.
   */
  @NotNull
  public Set<Position> paths() {
    return paths;
  }

  /**
   * Get the root node of this tree.
   */
  public @NotNull Optional<UpgradeNode> rootNode() {
    return Optional.ofNullable(nodes.get(rootNodeKey));
  }

  /**
   * Get a node by its key (short key, not full namespaced).
   */
  public @NotNull Optional<UpgradeNode> getNode(@NotNull String nodeKey) {
    return Optional.ofNullable(nodes.get(nodeKey));
  }

  /**
   * Get all nodes in this tree.
   */
  public @NotNull Collection<UpgradeNode> allNodes() {
    return Collections.unmodifiableCollection(nodes.values());
  }

  /**
   * Get the children nodes of a given node.
   */
  public @NotNull Collection<UpgradeNode> getChildren(@NotNull UpgradeNode node) {
    return node.children().stream()
        .map(nodes::get)
        .filter(n -> n != null)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Get nodes that are available for unlock given a set of already-unlocked nodes.
   * Uses a minimum gate for maxedPrerequisites: each listed node must be unlocked
   * (full max-level check happens at actual unlock time via the service).
   *
   * @param unlockedNodeKeys set of already unlocked node keys
   * @return nodes that can be unlocked next
   */
  public @NotNull Set<UpgradeNode> getAvailableNodes(@NotNull Set<String> unlockedNodeKeys) {
    return getAvailableNodes(unlockedNodeKeys, k -> 0);
  }

  /**
   * Get nodes that are available for unlock given a set of already-unlocked nodes
   * and a function providing each node's current level (for maxedPrerequisites check).
   *
   * @param unlockedNodeKeys set of already unlocked node keys
   * @param nodeLevelFn      function returning current level for a given node key
   * @return nodes that can be unlocked next
   */
  public @NotNull Set<UpgradeNode> getAvailableNodes(
      @NotNull Set<String> unlockedNodeKeys,
      @NotNull Function<String, Integer> nodeLevelFn
  ) {
    Set<UpgradeNode> available = new HashSet<>();

    for (UpgradeNode node : nodes.values()) {
      String nodeKey = getShortKey(node);

      // Skip already unlocked
      if (unlockedNodeKeys.contains(nodeKey)) {
        continue;
      }

      // Check if excluded by any already-unlocked node
      if (isExcludedByUnlockedNode(nodeKey, unlockedNodeKeys)) {
        continue;
      }

      // Check if all AND prerequisites are met
      boolean andPrereqsMet = node.prerequisites().isEmpty()
          || unlockedNodeKeys.containsAll(node.prerequisites());

      // Check if at least one OR prerequisite is met (or if there are none)
      boolean orPrereqsMet = node.prerequisitesOr().isEmpty()
          || node.prerequisitesOr().stream().anyMatch(unlockedNodeKeys::contains);

      // Check maxed prerequisites (all must be at max level)
      boolean maxedPrereqsMet = areMaxedPrereqsMet(node, unlockedNodeKeys, nodeLevelFn);

      if (andPrereqsMet && orPrereqsMet && maxedPrereqsMet) {
        available.add(node);
      }
    }

    return available;
  }

  /**
   * Check if a node can be unlocked.
   * Does not perform a max-level check on maxedPrerequisites — use the overload
   * accepting a nodeLevelFn for full validation.
   *
   * @param nodeKey          the node to check
   * @param unlockedNodeKeys currently unlocked nodes
   * @param availablePoints  skill points available
   * @return true if the node can be unlocked
   */
  public boolean canUnlock(
      @NotNull String nodeKey,
      @NotNull Set<String> unlockedNodeKeys,
      int availablePoints
  ) {
    return canUnlock(nodeKey, unlockedNodeKeys, availablePoints, k -> 1);
  }

  /**
   * Check if a node can be unlocked, including a max-level check on maxedPrerequisites.
   *
   * @param nodeKey          the node to check
   * @param unlockedNodeKeys currently unlocked nodes
   * @param availablePoints  skill points available
   * @param nodeLevelFn      function returning current level for a given node key
   * @return true if the node can be unlocked
   */
  public boolean canUnlock(
      @NotNull String nodeKey,
      @NotNull Set<String> unlockedNodeKeys,
      int availablePoints,
      @NotNull Function<String, Integer> nodeLevelFn
  ) {
    UpgradeNode node = nodes.get(nodeKey);
    if (node == null) {
      return false;
    }

    // Already unlocked
    if (unlockedNodeKeys.contains(nodeKey)) {
      return false;
    }

    // Check cost
    if (node.cost() > availablePoints) {
      return false;
    }

    // Check if excluded by any already-unlocked node
    if (isExcludedByUnlockedNode(nodeKey, unlockedNodeKeys)) {
      return false;
    }

    // Check AND prerequisites (all must be met)
    boolean andPrereqsMet = node.prerequisites().isEmpty()
        || unlockedNodeKeys.containsAll(node.prerequisites());

    // Check OR prerequisites (at least one must be met)
    boolean orPrereqsMet = node.prerequisitesOr().isEmpty()
        || node.prerequisitesOr().stream().anyMatch(unlockedNodeKeys::contains);

    // Check maxed prerequisites (all must be at max level)
    boolean maxedPrereqsMet = areMaxedPrereqsMet(node, unlockedNodeKeys, nodeLevelFn);

    return andPrereqsMet && orPrereqsMet && maxedPrereqsMet;
  }

  /**
   * Check whether all maxedPrerequisites of the given node are unlocked and at max level.
   *
   * @param node             the node whose maxedPrerequisites are checked
   * @param unlockedNodeKeys currently unlocked nodes
   * @param nodeLevelFn      function returning current level for a given node key
   * @return true if all maxed prerequisites are satisfied (or the list is empty)
   */
  public boolean areMaxedPrereqsMet(
      @NotNull UpgradeNode node,
      @NotNull Set<String> unlockedNodeKeys,
      @NotNull Function<String, Integer> nodeLevelFn
  ) {
    if (node.maxedPrerequisites().isEmpty()) {
      return true;
    }
    return node.maxedPrerequisites().stream().allMatch(k -> {
      if (!unlockedNodeKeys.contains(k)) {
        return false;
      }
      UpgradeNode prereqNode = nodes.get(k);
      if (prereqNode == null) {
        return false;
      }
      int currentLevel = nodeLevelFn.apply(k);
      return currentLevel >= prereqNode.maxLevel();
    });
  }

  /**
   * Check if a node is excluded by any already-unlocked node.
   * A node is excluded if any unlocked node lists it in its exclusive set.
   *
   * @param nodeKey          the node to check
   * @param unlockedNodeKeys currently unlocked nodes
   * @return true if this node is excluded by an unlocked node's exclusive set
   */
  public boolean isExcludedByUnlockedNode(@NotNull String nodeKey, @NotNull Set<String> unlockedNodeKeys) {
    for (String unlockedKey : unlockedNodeKeys) {
      UpgradeNode unlockedNode = nodes.get(unlockedKey);
      if (unlockedNode != null && unlockedNode.exclusive().contains(nodeKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the set of unlocked nodes that exclude a given node.
   *
   * @param nodeKey          the node to check
   * @param unlockedNodeKeys currently unlocked nodes
   * @return set of unlocked node keys that have this node in their exclusive set
   */
  public @NotNull Set<String> getExcludingNodes(@NotNull String nodeKey, @NotNull Set<String> unlockedNodeKeys) {
    Set<String> excluding = new HashSet<>();
    for (String unlockedKey : unlockedNodeKeys) {
      UpgradeNode unlockedNode = nodes.get(unlockedKey);
      if (unlockedNode != null && unlockedNode.exclusive().contains(nodeKey)) {
        excluding.add(unlockedKey);
      }
    }
    return excluding;
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }
}
