package net.aincraft.upgrade;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final String rootNodeKey;
  private final int skillPointsPerLevel;
  private final Map<String, UpgradeNode> nodes;
  private final Map<String, ConnectorNode> connectors;
  private final Map<String, PerkPolicy> perkPolicies;

  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes,
      @NotNull Map<String, ConnectorNode> connectors,
      @NotNull Map<String, PerkPolicy> perkPolicies
  ) {
    this.key = key;
    this.jobKey = jobKey;
    this.rootNodeKey = rootNodeKey;
    this.skillPointsPerLevel = skillPointsPerLevel;
    this.nodes = new HashMap<>(nodes);
    this.connectors = new HashMap<>(connectors);
    this.perkPolicies = new HashMap<>(perkPolicies);
  }

  /**
   * Constructor for backward compatibility (no connectors, no perk policies).
   */
  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes
  ) {
    this(key, jobKey, rootNodeKey, skillPointsPerLevel, nodes, new HashMap<>(), new HashMap<>());
  }

  /**
   * Constructor for backward compatibility (no perk policies).
   */
  public UpgradeTree(
      @NotNull Key key,
      @NotNull String jobKey,
      @NotNull String rootNodeKey,
      int skillPointsPerLevel,
      @NotNull Map<String, UpgradeNode> nodes,
      @NotNull Map<String, ConnectorNode> connectors
  ) {
    this(key, jobKey, rootNodeKey, skillPointsPerLevel, nodes, connectors, new HashMap<>());
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
   * Get all connector nodes in this tree.
   */
  public @NotNull Collection<ConnectorNode> allConnectors() {
    return Collections.unmodifiableCollection(connectors.values());
  }

  /**
   * Get a connector by its key.
   */
  public @NotNull Optional<ConnectorNode> getConnector(@NotNull String connectorKey) {
    return Optional.ofNullable(connectors.get(connectorKey));
  }

  /**
   * Get all specialization (major) nodes.
   */
  public @NotNull Collection<UpgradeNode> specializations() {
    return nodes.values().stream()
        .filter(UpgradeNode::isSpecialization)
        .collect(Collectors.toUnmodifiableList());
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
   *
   * @param unlockedNodeKeys set of already unlocked node keys
   * @return nodes that can be unlocked next
   */
  public @NotNull Set<UpgradeNode> getAvailableNodes(@NotNull Set<String> unlockedNodeKeys) {
    Set<UpgradeNode> available = new HashSet<>();

    for (UpgradeNode node : nodes.values()) {
      // Skip already unlocked
      if (unlockedNodeKeys.contains(getShortKey(node))) {
        continue;
      }

      // Check if any exclusive node is already unlocked
      boolean excludedByExclusive = node.exclusive().stream()
          .anyMatch(unlockedNodeKeys::contains);
      if (excludedByExclusive) {
        continue;
      }

      // Check if all prerequisites are met
      boolean prerequisitesMet = node.prerequisites().isEmpty()
          || unlockedNodeKeys.containsAll(node.prerequisites());
      if (prerequisitesMet) {
        available.add(node);
      }
    }

    return available;
  }

  /**
   * Check if a node can be unlocked.
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

    // Check exclusives
    boolean excludedByExclusive = node.exclusive().stream()
        .anyMatch(unlockedNodeKeys::contains);
    if (excludedByExclusive) {
      return false;
    }

    // Check prerequisites
    return node.prerequisites().isEmpty()
        || unlockedNodeKeys.containsAll(node.prerequisites());
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }
}
