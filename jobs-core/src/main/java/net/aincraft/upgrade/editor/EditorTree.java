package net.aincraft.upgrade.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeTree;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable upgrade tree for editing purposes.
 * Can be converted to/from immutable UpgradeTree.
 */
public final class EditorTree {

  private String treeId;
  private String displayName;
  private String jobKey;
  private int skillPointsPerLevel;
  private String rootNodeId;
  private final Map<String, EditorNode> nodes = new LinkedHashMap<>();
  private final List<EditorArchetype> archetypes = new ArrayList<>();
  private final Map<String, String> perkPolicies = new HashMap<>(); // perkId -> MAX/ADDITIVE
  private final List<Position> paths = new ArrayList<>(); // tree-level paths

  public EditorTree() {
    this.treeId = "new_tree";
    this.displayName = "New Tree";
    this.jobKey = "";
    this.skillPointsPerLevel = 1;
    this.rootNodeId = "";
  }

  /**
   * Create from an existing UpgradeTree.
   */
  public static EditorTree fromUpgradeTree(@NotNull UpgradeTree source) {
    EditorTree editor = new EditorTree();
    editor.treeId = extractSimpleKey(source.key().value());
    editor.displayName = extractSimpleKey(source.key().value()); // Use key as display name if not available
    editor.jobKey = source.jobKey();
    editor.skillPointsPerLevel = source.skillPointsPerLevel();
    editor.rootNodeId = source.rootNodeKey();

    // Import perk policies
    source.perkPolicies().forEach((k, v) -> editor.perkPolicies.put(k, v.name()));

    // Import paths
    editor.paths.addAll(source.paths());

    // Import nodes
    for (UpgradeNode node : source.allNodes()) {
      EditorNode editorNode = EditorNode.fromUpgradeNode(node);
      editor.nodes.put(editorNode.id(), editorNode);
    }

    return editor;
  }

  /**
   * Create a blank tree for a job.
   */
  public static EditorTree createBlank(@NotNull String jobKey) {
    EditorTree editor = new EditorTree();
    editor.treeId = jobKey + "_v1";
    editor.displayName = capitalize(jobKey) + " Tree";
    editor.jobKey = jobKey;

    // Create default root node
    EditorNode root = new EditorNode();
    root.setId(jobKey + "_basics");
    root.setName(capitalize(jobKey) + " Basics");
    root.setDescription("The foundation of " + jobKey + " knowledge");
    root.setIcon(Material.BOOK);
    root.setCost(0);
    root.setPosition(new Position(4, 0));
    editor.nodes.put(root.id(), root);
    editor.rootNodeId = root.id();

    // Create default archetypes
    editor.archetypes.add(new EditorArchetype("xp", "XP Focus", "green"));
    editor.archetypes.add(new EditorArchetype("money", "Money Focus", "gold"));
    editor.archetypes.add(new EditorArchetype("utility", "Utility", "blue"));

    return editor;
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  /**
   * Extract the simple key from a namespaced key (e.g., "upgrade_tree/miner" -> "miner").
   */
  private static String extractSimpleKey(String namespacedKey) {
    if (namespacedKey == null || namespacedKey.isEmpty()) return namespacedKey;
    int slashIndex = namespacedKey.indexOf('/');
    return slashIndex >= 0 ? namespacedKey.substring(slashIndex + 1) : namespacedKey;
  }

  // ========== Getters/Setters ==========

  public String treeId() { return treeId; }
  public void setTreeId(String treeId) { this.treeId = treeId; }

  public String displayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }

  public String jobKey() { return jobKey; }
  public void setJobKey(String jobKey) { this.jobKey = jobKey; }

  public int skillPointsPerLevel() { return skillPointsPerLevel; }
  public void setSkillPointsPerLevel(int sp) { this.skillPointsPerLevel = sp; }

  public String rootNodeId() { return rootNodeId; }
  public void setRootNodeId(String rootNodeId) { this.rootNodeId = rootNodeId; }

  public Map<String, EditorNode> nodes() { return nodes; }
  public List<EditorArchetype> archetypes() { return archetypes; }
  public Map<String, String> perkPolicies() { return perkPolicies; }
  public List<Position> paths() { return paths; }

  // ========== Node Operations ==========

  public Optional<EditorNode> getNode(String id) {
    return Optional.ofNullable(nodes.get(id));
  }

  public void addNode(EditorNode node) {
    nodes.put(node.id(), node);
  }

  public void removeNode(String id) {
    nodes.remove(id);
    // Also remove from other nodes' prerequisites/children
    for (EditorNode node : nodes.values()) {
      node.prerequisites().remove(id);
      node.children().remove(id);
      node.exclusive().remove(id);
    }
  }

  /**
   * Find a node at the given position.
   */
  public Optional<EditorNode> getNodeAtPosition(int x, int y) {
    for (EditorNode node : nodes.values()) {
      Position pos = node.position();
      if (pos != null && pos.x() == x && pos.y() == y) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  /**
   * Check if a position is occupied.
   */
  public boolean isPositionOccupied(int x, int y) {
    return getNodeAtPosition(x, y).isPresent();
  }

  /**
   * Generate a unique node ID.
   */
  public String generateNodeId() {
    int counter = nodes.size() + 1;
    String baseId = "node_" + counter;
    while (nodes.containsKey(baseId)) {
      counter++;
      baseId = "node_" + counter;
    }
    return baseId;
  }

  // ========== Copy/Restore ==========

  /**
   * Create a deep copy of this tree.
   */
  public EditorTree copy() {
    EditorTree copy = new EditorTree();
    copy.treeId = this.treeId;
    copy.displayName = this.displayName;
    copy.jobKey = this.jobKey;
    copy.skillPointsPerLevel = this.skillPointsPerLevel;
    copy.rootNodeId = this.rootNodeId;
    copy.perkPolicies.putAll(this.perkPolicies);
    copy.paths.addAll(this.paths);
    copy.archetypes.addAll(this.archetypes.stream().map(EditorArchetype::copy).toList());
    for (Map.Entry<String, EditorNode> entry : this.nodes.entrySet()) {
      copy.nodes.put(entry.getKey(), entry.getValue().copy());
    }
    return copy;
  }

  /**
   * Restore this tree's state from another tree.
   */
  public void restoreFrom(EditorTree source) {
    this.treeId = source.treeId;
    this.displayName = source.displayName;
    this.jobKey = source.jobKey;
    this.skillPointsPerLevel = source.skillPointsPerLevel;
    this.rootNodeId = source.rootNodeId;
    this.perkPolicies.clear();
    this.perkPolicies.putAll(source.perkPolicies);
    this.paths.clear();
    this.paths.addAll(source.paths);
    this.archetypes.clear();
    this.archetypes.addAll(source.archetypes.stream().map(EditorArchetype::copy).toList());
    this.nodes.clear();
    for (Map.Entry<String, EditorNode> entry : source.nodes.entrySet()) {
      this.nodes.put(entry.getKey(), entry.getValue().copy());
    }
  }

  // ========== Archetype Record ==========

  public record EditorArchetype(String id, String name, String color) {
    public EditorArchetype copy() {
      return new EditorArchetype(id, name, color);
    }
  }
}
