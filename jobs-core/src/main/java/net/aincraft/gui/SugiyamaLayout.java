package net.aincraft.gui;

import java.util.*;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeTree;

/**
 * Simplified Sugiyama layout for Wynncraft-style skill trees.
 *
 * This is the original Sugiyama algorithm WITHOUT crossing minimization.
 * The key insight: don't reorder nodes - trust the JSON order.
 *
 * Algorithm:
 * 1. Layer assignment - Y coordinates from tree depth
 * 2. X assignment - Main path centered, branches offset to sides (based on JSON order)
 *
 * This creates clean layouts where the first child continues the main path
 * and additional children are side branches.
 */
public class SugiyamaLayout {

  private static final int VERTICAL_SPACING = 1;   // Rows between layers
  private static final int CENTER_X = 4;            // Center column for main path
  private static final int BRANCH_SPACING = 2;      // Columns between branches

  /**
   * Generate positions for all nodes in the tree.
   * Returns a map of node key -> position.
   */
  public static Map<String, Position> generateLayout(UpgradeTree tree) {
    // Phase 1: Layer assignment (Y coordinates)
    Map<UpgradeNode, Integer> layers = assignLayers(tree);

    // Phase 2: X assignment based on child order
    return assignXCoordinates(layers, tree);
  }

  /**
   * Phase 1: Assign each node to a layer (Y coordinate) based on longest path from root.
   */
  private static Map<UpgradeNode, Integer> assignLayers(UpgradeTree tree) {
    Map<UpgradeNode, Integer> layers = new HashMap<>();

    // Find root node
    UpgradeNode root = tree.allNodes().stream()
        .filter(n -> tree.allNodes().stream()
            .noneMatch(parent -> parent.children().contains(getShortKey(n, tree))))
        .findFirst()
        .orElse(null);

    if (root == null) return layers;

    // Start with root at layer 0
    layers.put(root, 0);

    // Propagate layers through children using longest path
    boolean changed = true;
    while (changed) {
      changed = false;

      for (UpgradeNode node : tree.allNodes()) {
        if (!layers.containsKey(node)) continue;

        int nodeLayer = layers.get(node);
        for (String childKey : node.children()) {
          tree.getNode(childKey).ifPresent(child -> {
            int childLayer = nodeLayer + VERTICAL_SPACING;
            int currentLayer = layers.getOrDefault(child, -1);
            if (childLayer > currentLayer) {
              layers.put(child, childLayer);
            }
          });
        }
      }

      // Check if all nodes have layers assigned
      changed = tree.allNodes().stream().anyMatch(n -> !layers.containsKey(n));
    }

    return layers;
  }

  /**
   * Phase 2: Assign X coordinates based on tree structure.
   *
   * Rules:
   * - Root goes at center (x=4)
   * - First child of each node continues main path (same X as parent)
   * - Additional children are branches: alternate left/right
   * - Each branch maintains its X coordinate for all its descendants
   */
  private static Map<String, Position> assignXCoordinates(
      Map<UpgradeNode, Integer> layers,
      UpgradeTree tree) {

    Map<String, Position> positions = new HashMap<>();
    Map<UpgradeNode, Integer> nodeX = new HashMap<>(); // Store assigned X for each node

    // Find root
    UpgradeNode root = tree.allNodes().stream()
        .filter(n -> tree.allNodes().stream()
            .noneMatch(parent -> parent.children().contains(getShortKey(n, tree))))
        .findFirst()
        .orElse(null);

    if (root == null) return positions;

    // Root at center
    nodeX.put(root, CENTER_X);

    // BFS to assign X coordinates to children
    Queue<UpgradeNode> queue = new LinkedList<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      UpgradeNode parent = queue.poll();
      Integer parentX = nodeX.get(parent);
      if (parentX == null) continue;

      List<String> children = parent.children();
      if (children.isEmpty()) continue;

      // First child continues main path (same X as parent)
      String firstChildKey = children.get(0);
      tree.getNode(firstChildKey).ifPresent(firstChild -> {
        nodeX.put(firstChild, parentX);
        queue.add(firstChild);
      });

      // Additional children are branches
      int[] direction = {1}; // Start with right side (use array for lambda)
      for (int i = 1; i < children.size(); i++) {
        String childKey = children.get(i);
        tree.getNode(childKey).ifPresent(child -> {
          int branchX = parentX + (direction[0] * BRANCH_SPACING);

          // Clamp to bounds
          branchX = Math.max(0, Math.min(8, branchX));

          nodeX.put(child, branchX);
          queue.add(child);

          direction[0] *= -1; // Alternate sides for next branch
        });
      }
    }

    // Create positions from X and Y
    for (Map.Entry<UpgradeNode, Integer> entry : nodeX.entrySet()) {
      UpgradeNode node = entry.getKey();
      int x = entry.getValue();
      Integer y = layers.get(node);
      if (y != null) {
        positions.put(getShortKey(node), new Position(x, y));
      }
    }

    return positions;
  }

  /**
   * Get short key from node (strip namespace).
   */
  private static String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }

  /**
   * Get short key from node (with tree parameter for compatibility).
   */
  private static String getShortKey(UpgradeNode node, UpgradeTree tree) {
    return getShortKey(node);
  }
}
