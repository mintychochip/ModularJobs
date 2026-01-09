package net.aincraft.gui;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.UnlockResult;
import net.aincraft.upgrade.UpgradeTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * GUI for displaying and interacting with job upgrade trees.
 */
public final class UpgradeTreeGui implements Listener {

  private static final int GUI_SIZE = 54; // 6 rows
  private static final int GUI_ROWS = 6;
  private static final int GUI_COLS = 9;

  private final Plugin plugin;
  private final UpgradeService upgradeService;
  private final NamespacedKey nodeKeyTag;

  // Track open GUIs: player UUID -> session data
  private final Map<UUID, GuiSession> openGuis = new HashMap<>();

  // Store player inventories while GUI is open
  private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

  private static class GuiSession {
    final Job job;
    final UpgradeTree tree;
    int scrollOffset;

    GuiSession(Job job, UpgradeTree tree) {
      this.job = job;
      this.tree = tree;
      this.scrollOffset = 0;
    }
  }

  @Inject
  public UpgradeTreeGui(Plugin plugin, UpgradeService upgradeService) {
    this.plugin = plugin;
    this.upgradeService = upgradeService;
    this.nodeKeyTag = new NamespacedKey(plugin, "upgrade_node");
  }

  /**
   * Open the upgrade tree GUI for a player.
   */
  public void open(Player player, Job job, UpgradeTree tree) {
    PlayerUpgradeData data = upgradeService.getPlayerData(
        player.getUniqueId().toString(), job.key().value());

    Component title = Component.text()
        .append(job.displayName())
        .append(Component.text(" Upgrades", NamedTextColor.GRAY))
        .build();

    Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

    // Create and store session
    UUID playerId = player.getUniqueId();
    GuiSession session = new GuiSession(job, tree);
    openGuis.put(playerId, session);

    // Fill background with glass panes
    fillBackground(gui);

    // Place nodes based on their positions (with scroll offset)
    renderNodes(gui, session, data);

    // Save and clear player inventory, then add navigation arrows and info book
    savedInventories.put(playerId, player.getInventory().getContents().clone());
    player.getInventory().clear();
    updateNavigationArrows(player, session, data);

    player.openInventory(gui);
  }

  /**
   * Refresh the GUI for a player (after unlocking a node).
   * Updates the inventory in-place to avoid cursor movement.
   */
  public void refresh(Player player) {
    UUID playerId = player.getUniqueId();
    GuiSession session = openGuis.get(playerId);
    if (session == null) {
      return;
    }

    // Get the player's open inventory
    Inventory gui = player.getOpenInventory().getTopInventory();
    if (gui.getSize() != GUI_SIZE) {
      return; // Not our GUI
    }

    // Update player data
    PlayerUpgradeData data = upgradeService.getPlayerData(
        player.getUniqueId().toString(), session.job.key().value());

    // Re-render nodes with current scroll offset
    fillBackground(gui);
    renderNodes(gui, session, data);

    // Update navigation arrows and info book
    updateNavigationArrows(player, session, data);
  }

  /**
   * Render nodes in the GUI with the current scroll offset.
   */
  private void renderNodes(Inventory gui, GuiSession session, PlayerUpgradeData data) {
    Set<String> unlocked = data.unlockedNodes();
    Set<UpgradeNode> available = session.tree.getAvailableNodes(unlocked);

    // First, render connection lines between nodes
    renderConnections(gui, session, unlocked);

    // Then render connector nodes (so they appear below ability nodes)
    for (net.aincraft.upgrade.ConnectorNode connector : session.tree.allConnectors()) {
      int slot = calculateSlotWithScroll(connector.position(), session.scrollOffset);
      if (slot < 0 || slot >= GUI_SIZE) {
        continue; // Outside visible area
      }
      renderConnector(gui, connector, unlocked, session.tree);
    }

    // Finally render ability nodes (so they appear on top of connectors)
    for (UpgradeNode node : session.tree.allNodes()) {
      int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
      if (slot < 0 || slot >= GUI_SIZE) {
        continue; // Outside visible area
      }
      NodeStatus status = getNodeStatus(node, unlocked, available);
      ItemStack item = createNodeItem(node, status, data, session.tree);
      gui.setItem(slot, item);
    }
  }

  /**
   * Render connection lines between parent and child nodes.
   * Deduplicates segments to prevent overlapping paths and illegal T-junctions.
   */
  private void renderConnections(Inventory gui, GuiSession session, Set<String> unlocked) {
    // Collect all path segments with their unlock status
    Map<GridPoint, PathSegment> segments = new HashMap<>();

    for (UpgradeNode node : session.tree.allNodes()) {
      String nodeKey = getShortKey(node);
      boolean nodeUnlocked = unlocked.contains(nodeKey);

      // Process connections to children
      for (String childKey : node.children()) {
        UpgradeNode child = session.tree.getNode(childKey).orElse(null);
        if (child == null || child.position() == null || node.position() == null) {
          continue; // Skip null nodes and nodes without positions
        }

        boolean childUnlocked = unlocked.contains(childKey);
        boolean pathUnlocked = nodeUnlocked && childUnlocked;

        // Build path from parent position through child's path points to child position
        List<GridPoint> path = new ArrayList<>();

        // Start at parent position (adjusted for scroll)
        int parentX = node.position().x();
        int parentY = node.position().y() - session.scrollOffset;
        path.add(new GridPoint(parentX, parentY));

        // Add child's explicit path points (adjusted for scroll)
        for (Position pathPoint : child.pathPoints()) {
          int x = pathPoint.x();
          int y = pathPoint.y() - session.scrollOffset;
          path.add(new GridPoint(x, y));
        }

        // End at child position (adjusted for scroll)
        int childX = child.position().x();
        int childY = child.position().y() - session.scrollOffset;
        path.add(new GridPoint(childX, childY));

        // Add path segments (skip start node, include path points and end at child-1)
        for (int i = 1; i < path.size() - 1; i++) {
          GridPoint point = path.get(i);
          if (point.y >= 0 && point.y < GUI_ROWS) {
            // If segment already exists, upgrade it to unlocked if this path is unlocked
            PathSegment existing = segments.get(point);
            if (existing == null || (pathUnlocked && !existing.unlocked)) {
              segments.put(point, new PathSegment(point, pathUnlocked, path, i));
            }
          }
        }
      }
    }

    // Draw all unique segments
    for (PathSegment segment : segments.values()) {
      drawSegment(gui, segment);
    }
  }

  /**
   * Draw a single path segment at a specific position.
   */
  private void drawSegment(Inventory gui, PathSegment segment) {
    Material lineMaterial = segment.unlocked ? Material.CYAN_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
    String segmentType = detectSegmentType(segment.fullPath, segment.indexInPath);

    ItemStack lineItem = new ItemStack(lineMaterial);
    ItemMeta meta = lineItem.getItemMeta();
    meta.displayName(Component.text(segmentType, NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false));
    lineItem.setItemMeta(meta);

    int slot = segment.point.y * GUI_COLS + segment.point.x;
    if (slot >= 0 && slot < GUI_SIZE) {
      gui.setItem(slot, lineItem);
    }
  }

  /**
   * Render a CONNECTOR node with state-based icon.
   * Connector is unlocked when BOTH linked nodes are unlocked.
   */
  private void renderConnector(Inventory gui, net.aincraft.upgrade.ConnectorNode connector, Set<String> unlocked,
                               UpgradeTree tree) {
    // Get linked nodes
    List<String> links = connector.links();
    if (links.size() < 2) {
      return; // Invalid connector - needs at least 2 links
    }

    // Connector is unlocked if ALL linked nodes are unlocked
    boolean connectorUnlocked = unlocked.containsAll(links);

    // Create item based on state
    Material icon = connectorUnlocked ? connector.unlockedIcon() : connector.icon();
    int cmd = connectorUnlocked ? connector.unlockedCustomModelData() : connector.lockedCustomModelData();

    ItemStack item = new ItemStack(icon);
    ItemMeta meta = item.getItemMeta();

    // Set custom model data for resource pack icons
    if (cmd > 0) {
      meta.setCustomModelData(cmd);
    }

    // Set display name
    NamedTextColor nameColor = connectorUnlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY;
    meta.displayName(Component.text("Path", nameColor)
        .decoration(TextDecoration.ITALIC, false));

    // Build lore
    List<Component> lore = new ArrayList<>();

    // Show linked nodes
    lore.add(Component.text("Connects:", NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    for (String link : links) {
      // Get short key for display
      String shortLink = link.contains(":") ? link.substring(link.indexOf(':') + 1) : link;

      // Check if this linked node is unlocked
      boolean linkUnlocked = unlocked.contains(link);
      NamedTextColor linkColor = linkUnlocked ? NamedTextColor.GREEN : NamedTextColor.RED;

      lore.add(Component.text("  " + (linkUnlocked ? "✓" : "✗") + " " + shortLink, linkColor)
          .decoration(TextDecoration.ITALIC, false));
    }

    lore.add(Component.empty());

    // Status message
    Component status = connectorUnlocked
        ? Component.text("Path Unlocked", NamedTextColor.GREEN)
        : Component.text("Path Locked", NamedTextColor.GRAY);
    lore.add(status.decoration(TextDecoration.ITALIC, false));

    meta.lore(lore);
    item.setItemMeta(meta);

    // Place at connector's position
    Position pos = connector.position();
    int slot = pos.y() * GUI_COLS + pos.x();
    if (slot >= 0 && slot < GUI_SIZE) {
      gui.setItem(slot, item);
    }
  }

  /**
   * Represents a path segment at a specific position.
   */
  private static class PathSegment {
    final GridPoint point;
    final boolean unlocked;
    final List<GridPoint> fullPath;
    final int indexInPath;

    PathSegment(GridPoint point, boolean unlocked, List<GridPoint> fullPath, int indexInPath) {
      this.point = point;
      this.unlocked = unlocked;
      this.fullPath = fullPath;
      this.indexInPath = indexInPath;
    }
  }


  /**
   * Create Wynncraft-style path: go horizontal first, then vertical DOWN.
   * Only creates valid downward-flowing joints: ┐ ┌ │ ─
   * Pattern: (x1,y1) → horizontal → (x2,y1) [corner] → vertical DOWN → (x2,y2)
   *
   * IMPORTANT: Corners (┐ ┌) are only valid if within 1 space of an endpoint.
   * Longer L-shaped paths require explicit connector nodes and are NOT rendered.
   */
  private List<GridPoint> createWynnPath(int x1, int y1, int x2, int y2) {
    List<GridPoint> path = new ArrayList<>();

    // VALIDATION: Ensure path only goes down (y2 >= y1)
    if (y2 < y1) {
      plugin.getLogger().warning(String.format(
          "Invalid path: cannot go upward from (%d,%d) to (%d,%d)", x1, y1, x2, y2));
      // Return empty path - don't render invalid paths
      return path;
    }

    // VALIDATION: If both x and y differ, check if corner is within 1 space of an endpoint
    // Otherwise, this needs an explicit connector node (not auto-rendered)
    int cornerX = x2;
    int cornerY = y1;
    if (x1 != x2 && y1 != y2) {
      int distFromStart = Math.abs(cornerX - x1) + Math.abs(cornerY - y1);
      int distFromEnd = Math.abs(cornerX - x2) + Math.abs(cornerY - y2);
      if (distFromStart > 2 || distFromEnd > 2) {
        // Corner is too far - skip this path (requires connector node)
        return path;
      }
    }

    // Start point
    path.add(new GridPoint(x1, y1));

    // Horizontal segment from x1 to x2 at y1 (only if x changes)
    if (x1 != x2) {
      int xDir = x2 > x1 ? 1 : -1;
      for (int x = x1 + xDir; x != x2; x += xDir) {
        path.add(new GridPoint(x, y1));
      }
      // Corner point at (x2, y1) - will be either ┐ (right-down) or ┌ (left-down)
      path.add(new GridPoint(x2, y1));
    }

    // Vertical segment from y1 DOWN to y2 at x2 (only if y changes)
    if (y1 != y2) {
      // Always go DOWN (y increases)
      for (int y = y1 + 1; y != y2; y++) {
        path.add(new GridPoint(x2, y));
      }
    }

    // End point (only add if not already added)
    if (x1 != x2 || y1 != y2) {
      path.add(new GridPoint(x2, y2));
    }

    return path;
  }

  /**
   * Detect specific joint type: ┐ (right-down), ┌ (left-down), │ (vertical), ─ (horizontal), ┬ (T-down)
   * These are the ONLY valid joints for a downward-flowing tree.
   */
  private String detectSegmentType(List<GridPoint> path, int index) {
    if (index <= 0 || index >= path.size() - 1) {
      return "Path";
    }

    GridPoint prev = path.get(index - 1);
    GridPoint current = path.get(index);
    GridPoint next = path.get(index + 1);

    // Direction from prev to current
    int dx1 = current.x - prev.x;
    int dy1 = current.y - prev.y;

    // Direction from current to next
    int dx2 = next.x - current.x;
    int dy2 = next.y - current.y;

    // Check if direction changes (indicates a corner)
    if (dx1 != dx2 || dy1 != dy2) {
      // Corner detected - determine which type
      // Coming from horizontal, going vertical down
      if (dy1 == 0 && dy2 > 0) {
        if (dx1 > 0) {
          // Moving RIGHT then DOWN: →┐ = Right-Down
          return "┐ Right-Down";
        } else if (dx1 < 0) {
          // Moving LEFT then DOWN: ┌← = Left-Down
          return "┌ Left-Down";
        }
      }
      // Invalid corner (going up or other)
      return "⚠ Invalid Corner";
    }

    // Straight segments
    if (dx1 != 0 && dy1 == 0) {
      return "─ Horizontal";
    } else if (dx1 == 0 && dy1 > 0) {
      return "│ Vertical Down";
    } else if (dx1 == 0 && dy1 < 0) {
      return "⚠ Vertical Up"; // Invalid!
    }

    return "Path";
  }

  /**
   * A* pathfinding to find optimal path between two points.
   * Only uses cardinal directions (no diagonals).
   */
  private List<GridPoint> findPath(int startX, int startY, int endX, int endY, int scrollOffset, UpgradeTree tree) {
    // Build set of obstacle positions (all node positions except start and end)
    Set<GridPoint> obstacles = new HashSet<>();
    for (UpgradeNode node : tree.allNodes()) {
      if (node.position() != null) {
        int nodeX = node.position().x();
        int nodeY = node.position().y() - scrollOffset;
        GridPoint point = new GridPoint(nodeX, nodeY);
        // Don't mark start or end as obstacles
        if (!point.equals(new GridPoint(startX, startY)) && !point.equals(new GridPoint(endX, endY))) {
          obstacles.add(point);
        }
      }
    }

    // A* algorithm
    PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
    Map<GridPoint, AStarNode> allNodes = new HashMap<>();

    GridPoint start = new GridPoint(startX, startY);
    GridPoint end = new GridPoint(endX, endY);

    AStarNode startNode = new AStarNode(start, 0, manhattanDistance(start, end), null);
    openSet.add(startNode);
    allNodes.put(start, startNode);

    while (!openSet.isEmpty()) {
      AStarNode current = openSet.poll();

      if (current.point.equals(end)) {
        // Reconstruct path
        List<GridPoint> path = new ArrayList<>();
        AStarNode node = current;
        while (node != null) {
          path.add(0, node.point);
          node = node.parent;
        }
        return path;
      }

      // Check all 4 cardinal neighbors
      int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // down, up, right, left
      for (int[] dir : directions) {
        int newX = current.point.x + dir[0];
        int newY = current.point.y + dir[1];

        // Check bounds
        if (newX < 0 || newX >= GUI_COLS || newY < 0 || newY >= GUI_ROWS) {
          continue;
        }

        GridPoint neighbor = new GridPoint(newX, newY);

        // Skip if obstacle (unless it's the end point)
        if (obstacles.contains(neighbor) && !neighbor.equals(end)) {
          continue;
        }

        int tentativeG = current.gScore + 1;

        AStarNode neighborNode = allNodes.get(neighbor);
        if (neighborNode == null) {
          neighborNode = new AStarNode(neighbor, Integer.MAX_VALUE, Integer.MAX_VALUE, null);
          allNodes.put(neighbor, neighborNode);
        }

        if (tentativeG < neighborNode.gScore) {
          neighborNode.parent = current;
          neighborNode.gScore = tentativeG;
          neighborNode.fScore = tentativeG + manhattanDistance(neighbor, end);

          openSet.remove(neighborNode);
          openSet.add(neighborNode);
        }
      }
    }

    // No path found, return null
    return null;
  }

  private int manhattanDistance(GridPoint a, GridPoint b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
  }

  private static class GridPoint {
    final int x, y;

    GridPoint(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof GridPoint)) return false;
      GridPoint that = (GridPoint) o;
      return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
      return 31 * x + y;
    }
  }

  private static class AStarNode {
    final GridPoint point;
    int gScore; // Cost from start
    int fScore; // gScore + heuristic
    AStarNode parent;

    AStarNode(GridPoint point, int gScore, int fScore, AStarNode parent) {
      this.point = point;
      this.gScore = gScore;
      this.fScore = fScore;
      this.parent = parent;
    }
  }

  /**
   * Update navigation arrows and info book in the player's bottom inventory.
   */
  private void updateNavigationArrows(Player player, GuiSession session, PlayerUpgradeData data) {
    Inventory playerInv = player.getInventory();

    // Calculate max scroll based on tree bounds
    int maxY = session.tree.allNodes().stream()
        .map(UpgradeNode::position)
        .filter(pos -> pos != null)
        .mapToInt(Position::y)
        .max()
        .orElse(0);

    int maxScroll = Math.max(0, maxY - GUI_ROWS + 1);
    boolean canScrollUp = session.scrollOffset > 0;
    boolean canScrollDown = session.scrollOffset < maxScroll;

    // Debug info
    plugin.getLogger().info(String.format(
        "Scroll Debug - MaxY: %d, MaxScroll: %d, CurrentOffset: %d, CanUp: %s, CanDown: %s",
        maxY, maxScroll, session.scrollOffset, canScrollUp, canScrollDown
    ));

    // Navigation arrows with cyan/blue theme
    // Up arrow in slot 0 (bottom-left of player inventory)
    ItemStack upArrow = new ItemStack(canScrollUp ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta upMeta = upArrow.getItemMeta();
    upMeta.displayName(Component.text(
        canScrollUp ? "Scroll Up" : "Scroll Up (At Top)",
        canScrollUp ? NamedTextColor.AQUA : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    if (canScrollUp) {
      upMeta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "scroll_action"),
          PersistentDataType.STRING,
          "up"
      );
    }
    upArrow.setItemMeta(upMeta);
    playerInv.setItem(0, upArrow);

    // Down arrow in slot 8 (bottom-right of player inventory)
    ItemStack downArrow = new ItemStack(canScrollDown ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta downMeta = downArrow.getItemMeta();
    downMeta.displayName(Component.text(
        canScrollDown ? "Scroll Down" : "Scroll Down (At Bottom)",
        canScrollDown ? NamedTextColor.AQUA : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    if (canScrollDown) {
      downMeta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "scroll_action"),
          PersistentDataType.STRING,
          "down"
      );
    }
    downArrow.setItemMeta(downMeta);
    playerInv.setItem(8, downArrow);

    // Info book in center of hotbar (slot 4)
    playerInv.setItem(4, createInfoItem(session.job, session.tree, data));
  }

  private void fillBackground(Inventory gui) {
    ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(" "));
    pane.setItemMeta(meta);

    for (int i = 0; i < GUI_SIZE; i++) {
      gui.setItem(i, pane);
    }
  }

  private NodeStatus getNodeStatus(UpgradeNode node, Set<String> unlocked, Set<UpgradeNode> available) {
    String shortKey = getShortKey(node);

    // Check if unlocked
    if (unlocked.contains(shortKey)) {
      return NodeStatus.UNLOCKED;
    }

    // Check if excluded by an exclusive node that's already unlocked
    for (String exclusiveKey : node.exclusive()) {
      if (unlocked.contains(exclusiveKey)) {
        return NodeStatus.EXCLUDED;
      }
    }

    // Check if available to unlock
    if (available.contains(node)) {
      return NodeStatus.AVAILABLE;
    }

    return NodeStatus.LOCKED;
  }

  private int calculateSlot(Position position) {
    if (position == null) {
      return -1; // No position defined
    }
    int x = position.x();
    int y = position.y();

    // Validate bounds
    if (x < 0 || x >= GUI_COLS || y < 0 || y >= GUI_ROWS) {
      return -1;
    }

    return y * GUI_COLS + x;
  }

  /**
   * Calculate slot position with scroll offset applied.
   */
  private int calculateSlotWithScroll(Position position, int scrollOffset) {
    if (position == null) {
      return -1;
    }
    int x = position.x();
    int y = position.y() - scrollOffset; // Apply scroll offset to Y coordinate

    // Validate bounds after scrolling
    if (x < 0 || x >= GUI_COLS || y < 0 || y >= GUI_ROWS) {
      return -1; // Outside visible area
    }

    return y * GUI_COLS + x;
  }

  private ItemStack createNodeItem(UpgradeNode node, NodeStatus status, PlayerUpgradeData data, UpgradeTree tree) {
    Material material = switch (status) {
      case UNLOCKED -> node.icon();
      case AVAILABLE -> node.icon();
      case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE; // Lighter gray for locked nodes
      case EXCLUDED -> Material.RED_STAINED_GLASS_PANE; // Red pane instead of barrier for excluded
    };

    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    // Set display name with status color
    NamedTextColor nameColor = switch (status) {
      case UNLOCKED -> NamedTextColor.GREEN;
      case AVAILABLE -> NamedTextColor.YELLOW;
      case LOCKED -> NamedTextColor.GRAY;
      case EXCLUDED -> NamedTextColor.RED;
    };

    meta.displayName(Component.text(node.name(), nameColor)
        .decoration(TextDecoration.ITALIC, false));

    // Build lore
    List<Component> lore = new ArrayList<>();

    // Description
    if (node.description() != null && !node.description().isEmpty()) {
      lore.add(Component.text(node.description(), NamedTextColor.GRAY)
          .decoration(TextDecoration.ITALIC, false));
    }

    lore.add(Component.empty());

    // Cost
    lore.add(Component.text()
        .append(Component.text("Cost: ", NamedTextColor.GRAY))
        .append(Component.text(node.cost() + " SP", NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .build());

    // Effects
    if (!node.effects().isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Effects:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      for (UpgradeEffect effect : node.effects()) {
        lore.add(Component.text("  \u2022 " + formatEffect(effect), NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    // Prerequisites for locked nodes
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Requires:", NamedTextColor.RED)
          .decoration(TextDecoration.ITALIC, false));
      for (String prereq : node.prerequisites()) {
        lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    lore.add(Component.empty());

    // Action hint
    Component actionHint = switch (status) {
      case UNLOCKED -> Component.text("\u2714 Unlocked!", NamedTextColor.GREEN);
      case AVAILABLE -> {
        if (data.availableSkillPoints() >= node.cost()) {
          yield Component.text("Click to unlock", NamedTextColor.YELLOW);
        } else {
          yield Component.text("Not enough SP!", NamedTextColor.RED);
        }
      }
      case LOCKED -> Component.text("Locked", NamedTextColor.DARK_GRAY);
      case EXCLUDED -> Component.text("\u2718 Path Locked (Exclusive Choice)", NamedTextColor.RED);
    };
    lore.add(actionHint.decoration(TextDecoration.ITALIC, false));

    // Show which exclusive node locked this path
    if (status == NodeStatus.EXCLUDED && !node.exclusive().isEmpty()) {
      List<String> exclusiveNames = node.exclusive().stream()
          .map(tree::getNode)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(UpgradeNode::name)
          .toList();
      lore.add(Component.text("Blocked by: " + String.join(", ", exclusiveNames), NamedTextColor.DARK_RED)
          .decoration(TextDecoration.ITALIC, false));
    }

    meta.lore(lore);

    // Add enchant glow for unlocked and available nodes
    if (status == NodeStatus.UNLOCKED || status == NodeStatus.AVAILABLE) {
      meta.addEnchant(Enchantment.UNBREAKING, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    // Store node key in PDC
    meta.getPersistentDataContainer().set(nodeKeyTag, PersistentDataType.STRING, getShortKey(node));

    item.setItemMeta(meta);
    return item;
  }

  private ItemStack createInfoItem(Job job, UpgradeTree tree, PlayerUpgradeData data) {
    ItemStack item = new ItemStack(Material.BOOK);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(Component.text("Skill Tree Info", NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false));

    List<Component> lore = new ArrayList<>();
    lore.add(Component.text()
        .append(Component.text("Available SP: ", NamedTextColor.GRAY))
        .append(Component.text(data.availableSkillPoints(), NamedTextColor.GREEN))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.text()
        .append(Component.text("Total SP: ", NamedTextColor.GRAY))
        .append(Component.text(data.totalSkillPoints(), NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.text()
        .append(Component.text("Unlocked: ", NamedTextColor.GRAY))
        .append(Component.text(data.unlockedNodes().size() + "/" + tree.allNodes().size(), NamedTextColor.YELLOW))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.empty());
    lore.add(Component.text()
        .append(Component.text("SP per level: ", NamedTextColor.GRAY))
        .append(Component.text(tree.skillPointsPerLevel(), NamedTextColor.WHITE))
        .decoration(TextDecoration.ITALIC, false)
        .build());

    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  private String formatEffect(UpgradeEffect effect) {
    return switch (effect) {
      case UpgradeEffect.BoostEffect boost ->
          String.format("+%.0f%% %s", (boost.multiplier().doubleValue() - 1) * 100, boost.target());
      case UpgradeEffect.RuledBoostEffect ruled -> {
        String desc = ruled.boostSource().description();
        yield desc != null ? desc : String.format("Conditional %s boost", ruled.target());
      }
      case UpgradeEffect.PermissionEffect perm ->
          String.format("Permission: %s", perm.permission());
    };
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    GuiSession session = openGuis.get(player.getUniqueId());
    if (session == null) {
      return;
    }

    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) {
      return;
    }

    ItemMeta meta = clicked.getItemMeta();
    if (meta == null) {
      return;
    }

    // Check for scroll action first
    NamespacedKey scrollKey = new NamespacedKey(plugin, "scroll_action");
    String scrollAction = meta.getPersistentDataContainer().get(scrollKey, PersistentDataType.STRING);
    if (scrollAction != null) {
      handleScroll(player, session, scrollAction);
      return;
    }

    // Check for node click
    String nodeKey = meta.getPersistentDataContainer().get(nodeKeyTag, PersistentDataType.STRING);
    if (nodeKey == null) {
      return; // Clicked on non-node item (background, info)
    }

    // Attempt to unlock
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();

    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);

    switch (result) {
      case UnlockResult.Success success -> {
        Mint.sendThemedMessage(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        // Refresh the GUI
        refresh(player);
      }
      case UnlockResult.InsufficientPoints ip -> {
        Mint.sendThemedMessage(player, "<error>Not enough SP! Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.PrerequisitesNotMet pm -> {
        Mint.sendThemedMessage(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pm.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Mint.sendThemedMessage(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.AlreadyUnlocked au -> {
        // Already unlocked - play a subtle click sound
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      }
      case UnlockResult.NodeNotFound nf -> {
        Mint.sendThemedMessage(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case UnlockResult.TreeNotFound tf -> {
        Mint.sendThemedMessage(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
  }

  /**
   * Handle scroll action from navigation arrows.
   */
  private void handleScroll(Player player, GuiSession session, String action) {
    // Calculate max scroll
    int maxY = session.tree.allNodes().stream()
        .map(UpgradeNode::position)
        .filter(pos -> pos != null)
        .mapToInt(Position::y)
        .max()
        .orElse(0);
    int maxScroll = Math.max(0, maxY - GUI_ROWS + 1);

    // Update scroll offset
    if ("up".equals(action) && session.scrollOffset > 0) {
      session.scrollOffset--;
    } else if ("down".equals(action) && session.scrollOffset < maxScroll) {
      session.scrollOffset++;
    } else {
      return; // No change needed
    }

    // Refresh the GUI with new scroll position
    refresh(player);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player) {
      UUID playerId = player.getUniqueId();
      openGuis.remove(playerId);

      // Restore player inventory
      ItemStack[] saved = savedInventories.remove(playerId);
      if (saved != null) {
        player.getInventory().setContents(saved);
      }
    }
  }

  private enum NodeStatus {
    UNLOCKED, AVAILABLE, LOCKED, EXCLUDED
  }
}
