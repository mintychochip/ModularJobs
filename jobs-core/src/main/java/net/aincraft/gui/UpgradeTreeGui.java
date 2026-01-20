package net.aincraft.gui;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
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
import static net.aincraft.gui.TexturedTitle.UPGRADE_TREE_BG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.event.player.PlayerQuitEvent;
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

  private static final int GUI_SIZE = 54; // 6 rows total
  private static final int GUI_ROWS = 6; // All 6 rows available for node rendering
  private static final int GUI_COLS = 9;

  private final Plugin plugin;
  private final UpgradeService upgradeService;
  private final NamespacedKey nodeKeyTag;

  // Track open GUIs: player UUID -> session data
  private final Map<UUID, GuiSession> openGuis = new HashMap<>();

  // Player inventory slots for navigation and info (top row of main inventory)
  // Slots 9-17 are the top row of main inventory, columns are 0-indexed
  private static final int NAV_UP_SLOT = 9 + 3;   // Top row, column 3 (4th from left)
  private static final int NAV_INFO_SLOT = 9 + 4; // Top row, column 4 (center, between arrows)
  private static final int NAV_DOWN_SLOT = 9 + 5; // Top row, column 5 (6th from left)

  private static class GuiSession {
    final Job job;
    final UpgradeTree tree;
    int scrollOffset;
    // Saved player inventory contents to restore on close
    ItemStack[] savedInventory;

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

  // Toggle for textured titles (enable when resource pack is ready)
  private static final boolean USE_TEXTURED_TITLE = true;

  /**
   * Open the upgrade tree GUI for a player.
   */
  public void open(Player player, Job job, UpgradeTree tree) {
    PlayerUpgradeData data = upgradeService.getPlayerData(
        player.getUniqueId().toString(), job.key().value());

    // Build title - use textured version if enabled, otherwise fallback to plain text
    Component title;
    if (USE_TEXTURED_TITLE) {
      // Textured title with custom background and overlay text
      // textureOffset -8 aligns the 256px background texture to the left edge of the inventory
      title = TexturedTitle.builder()
          .texture(TexturedTitle.UPGRADE_TREE_BG)
          .textureOffset(-8)
          .text(PlainTextComponentSerializer.plainText().serialize(job.displayName()) + " Upgrades", NamedTextColor.WHITE)
          .textOffset(-160) // Position text after the texture
          .build();
    } else {
      // Fallback plain text title
      title = Component.text()
          .append(job.displayName())
          .append(Component.text(" Upgrades", NamedTextColor.GRAY))
          .build();
    }

    Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

    // Create and store session
    UUID playerId = player.getUniqueId();
    GuiSession session = new GuiSession(job, tree);
    openGuis.put(playerId, session);

    // Save and clear player's entire inventory (will be restored on close)
    session.savedInventory = player.getInventory().getContents().clone();
    player.getInventory().clear();

    // Fill background with glass panes
    fillBackground(gui);

    // Place nodes based on their positions (with scroll offset)
    renderNodes(gui, session, data);

    // Add info book to control row, arrows go in player hotbar
    updateNavigationArrows(gui, session, data, player);

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

    // Update navigation arrows in player hotbar and info book in control row
    updateNavigationArrows(gui, session, data, player);
  }

  /**
   * Render nodes in the GUI with the current scroll offset.
   */
  private void renderNodes(Inventory gui, GuiSession session, PlayerUpgradeData data) {
    Set<String> unlocked = data.unlockedNodes();
    Set<UpgradeNode> available = session.tree.getAvailableNodes(unlocked);

    // First, render connection lines between nodes
    renderConnections(gui, session, unlocked, available);

    // Then render ability nodes
    for (UpgradeNode node : session.tree.allNodes()) {
      int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
      if (slot < 0 || slot >= GUI_SIZE) {
        continue; // Outside visible area
      }
      NodeStatus status = getNodeStatus(node, unlocked, available, session.tree);
      ItemStack item = createNodeItem(node, status, data, session.tree);
      gui.setItem(slot, item);
    }
  }

  /**
   * Render connection lines using flood-fill from unlocked nodes.
   * Path segments light up only if they lead to immediately available (unlockable) nodes.
   * BFS is done in absolute coordinates to find connections across pages.
   */
  private void renderConnections(Inventory gui, GuiSession session, Set<String> unlocked, Set<UpgradeNode> available) {
    int scrollOffset = session.scrollOffset;

    // Step 1: Collect all path points in ABSOLUTE coordinates (no scroll offset)
    Set<GridPoint> allPathPoints = new HashSet<>();
    for (Position p : session.tree.paths()) {
      allPathPoints.add(new GridPoint(p.x(), p.y()));
    }

    // Step 2: Collect node positions in ABSOLUTE coordinates
    Set<GridPoint> unlockedNodePositions = new HashSet<>();
    Set<GridPoint> allNodePositions = new HashSet<>();
    for (UpgradeNode node : session.tree.allNodes()) {
      if (node.position() == null) continue;
      GridPoint point = new GridPoint(node.position().x(), node.position().y());
      allNodePositions.add(point);
      String nodeKey = getShortKey(node);
      if (unlocked.contains(nodeKey)) {
        unlockedNodePositions.add(point);
      }
    }

    // Step 3: Find paths that connect unlocked nodes using BFS in absolute coordinates
    Set<GridPoint> litPathPoints = new HashSet<>();
    int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    // BFS from each unlocked node to find connections to other unlocked nodes
    for (GridPoint startNode : unlockedNodePositions) {
      Map<GridPoint, GridPoint> parent = new HashMap<>();
      java.util.Queue<GridPoint> queue = new java.util.LinkedList<>();
      Set<GridPoint> visited = new HashSet<>();

      queue.add(startNode);
      visited.add(startNode);

      while (!queue.isEmpty()) {
        GridPoint current = queue.poll();

        for (int[] dir : directions) {
          GridPoint neighbor = new GridPoint(current.x + dir[0], current.y + dir[1]);

          if (visited.contains(neighbor)) continue;
          visited.add(neighbor);
          parent.put(neighbor, current);

          // If neighbor is an unlocked node, trace back and light the connecting path
          if (unlockedNodePositions.contains(neighbor)) {
            GridPoint trace = neighbor;
            while (trace != null && parent.containsKey(trace)) {
              GridPoint prev = parent.get(trace);
              if (allPathPoints.contains(trace)) {
                litPathPoints.add(trace);
              }
              trace = prev;
            }
            queue.add(neighbor); // Continue to find more connections
          }
          // If neighbor is a path point, continue BFS (but don't light yet)
          else if (allPathPoints.contains(neighbor)) {
            queue.add(neighbor);
          }
          // If neighbor is a non-unlocked node (available/locked), stop traversal
        }
      }
    }

    // Step 4: Draw only VISIBLE path points (apply scroll offset for rendering)
    // Combine path points and node positions for neighbor detection
    Set<GridPoint> allConnectedPoints = new HashSet<>();
    allConnectedPoints.addAll(allPathPoints);
    allConnectedPoints.addAll(allNodePositions);

    // Combine lit path points and unlocked nodes for "active direction" detection
    Set<GridPoint> litPoints = new HashSet<>();
    litPoints.addAll(litPathPoints);
    litPoints.addAll(unlockedNodePositions);

    for (GridPoint pathPoint : allPathPoints) {
      int screenY = pathPoint.y - scrollOffset;
      // Only render if visible on current page
      if (screenY < 0 || screenY >= GUI_ROWS || pathPoint.x < 0 || pathPoint.x >= GUI_COLS) {
        continue;
      }

      boolean isLit = litPathPoints.contains(pathPoint);
      int branchType = detectBranchType(pathPoint, allConnectedPoints);

      // Determine which directions are "active" (lead to lit points or unlocked nodes)
      boolean litN = litPoints.contains(new GridPoint(pathPoint.x, pathPoint.y - 1));
      boolean litS = litPoints.contains(new GridPoint(pathPoint.x, pathPoint.y + 1));
      boolean litW = litPoints.contains(new GridPoint(pathPoint.x - 1, pathPoint.y));
      boolean litE = litPoints.contains(new GridPoint(pathPoint.x + 1, pathPoint.y));

      float customModelDataValue = getBranchCustomModelData(branchType, isLit, litN, litS, litE, litW);

      ItemStack lineItem = new ItemStack(Material.PAPER);
      ItemMeta meta = lineItem.getItemMeta();
      meta.setHideTooltip(true);
      lineItem.setItemMeta(meta);
      // Use new Data Component API for custom model data (1.21.4+)
      lineItem.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
          CustomModelData.customModelData().addFloat(customModelDataValue).build());

      int slot = screenY * GUI_COLS + pathPoint.x;
      if (slot >= 0 && slot < GUI_SIZE) {
        gui.setItem(slot, lineItem);
      }
    }
  }

  // Node texture color constants for CustomModelData thresholds
  // Each color has 4 states: locked, available (pulse), active, fragment
  // Threshold = colorBase + stateOffset (locked=0, available=1, active=2, fragment=3)
  private static final Map<String, Float> NODE_TEXTURE_BASE = Map.of(
      "blue", 1.0f,
      "purple", 5.0f,
      "red", 9.0f,
      "white", 13.0f,
      "yellow", 17.0f,
      "warrior", 21.0f
  );

  private static final int NODE_STATE_LOCKED = 0;
  private static final int NODE_STATE_AVAILABLE = 1;  // With pulse overlay
  private static final int NODE_STATE_ACTIVE = 2;
  private static final int NODE_STATE_FRAGMENT = 3;

  // Branch type constants (direction-based)
  private static final int BRANCH_CROSS = 0;      // N, E, S, W (4-way)
  private static final int BRANCH_T_NO_S = 1;     // N, E, W (T-junction, no South)
  private static final int BRANCH_T_NO_W = 2;     // N, E, S (T-junction, no West)
  private static final int BRANCH_T_NO_N = 3;     // W, E, S (T-junction, no North)
  private static final int BRANCH_T_NO_E = 4;     // N, W, S (T-junction, no East)
  private static final int BRANCH_CORNER_NW = 5;  // N, W (corner top-left)
  private static final int BRANCH_CORNER_NE = 6;  // N, E (corner top-right)
  private static final int BRANCH_CORNER_SE = 7;  // S, E (corner bottom-right)
  private static final int BRANCH_CORNER_SW = 8;  // W, S (corner bottom-left)
  private static final int BRANCH_VERTICAL = 9;   // N, S
  private static final int BRANCH_HORIZONTAL = 10; // E, W

  /**
   * Detect branch type based on neighboring connected points.
   * Uses direction-based detection: N=up, S=down, E=right, W=left
   */
  private int detectBranchType(GridPoint point, Set<GridPoint> allConnectedPoints) {
    boolean hasN = allConnectedPoints.contains(new GridPoint(point.x, point.y - 1));
    boolean hasS = allConnectedPoints.contains(new GridPoint(point.x, point.y + 1));
    boolean hasW = allConnectedPoints.contains(new GridPoint(point.x - 1, point.y));
    boolean hasE = allConnectedPoints.contains(new GridPoint(point.x + 1, point.y));

    int count = (hasN ? 1 : 0) + (hasS ? 1 : 0) + (hasW ? 1 : 0) + (hasE ? 1 : 0);

    // 4-way cross
    if (count == 4) return BRANCH_CROSS;  // 0

    // T-junctions (3 connections)
    if (count == 3) {
      if (!hasS) return BRANCH_T_NO_S;  // 1
      if (!hasW) return BRANCH_T_NO_W;  // 2
      if (!hasN) return BRANCH_T_NO_N;  // 3
      if (!hasE) return BRANCH_T_NO_E;  // 4
    }

    // Corners (2 connections, L-shape)
    if (count == 2) {
      if (hasN && hasW) return BRANCH_CORNER_NW;  // 5
      if (hasN && hasE) return BRANCH_CORNER_NE;  // 6
      if (hasS && hasE) return BRANCH_CORNER_SE;  // 7
      if (hasW && hasS) return BRANCH_CORNER_SW;  // 8
      if (hasN && hasS) return BRANCH_VERTICAL;   // 9
      if (hasE && hasW) return BRANCH_HORIZONTAL; // 10
    }

    // Single connection or edge cases - default to vertical/horizontal
    if (hasN || hasS) return BRANCH_VERTICAL;
    return BRANCH_HORIZONTAL;
  }

  /**
   * Get CustomModelData float value for branch type, active state, and lit directions.
   * Supports partial activation variants for cross and T-junction types.
   *
   * Threshold mapping:
   * - Type 0 (cross): 1-12 (1=inactive, 2-12=active variants 0-10)
   * - Type 1 (T no-S): 13-17 (13=inactive, 14-17=active variants 0-3)
   * - Type 2 (T no-W): 18-22 (18=inactive, 19-22=active variants 0-3)
   * - Type 3 (T no-N): 23-27 (23=inactive, 24-27=active variants 0-3)
   * - Type 4 (T no-E): 28-32 (28=inactive, 29-32=active variants 0-3)
   * - Type 5-10: 33-44 (2 each: inactive, active)
   */
  private float getBranchCustomModelData(int branchType, boolean isLit,
      boolean litN, boolean litS, boolean litE, boolean litW) {
    if (!isLit) {
      // Inactive - return base threshold for this type
      return getInactiveThreshold(branchType);
    }

    // Active - determine variant based on which directions are lit
    int variant = getActiveVariant(branchType, litN, litS, litE, litW);
    return getActiveThreshold(branchType, variant);
  }

  private float getInactiveThreshold(int branchType) {
    return switch (branchType) {
      case BRANCH_CROSS -> 1.0f;
      case BRANCH_T_NO_S -> 13.0f;
      case BRANCH_T_NO_W -> 18.0f;
      case BRANCH_T_NO_N -> 23.0f;
      case BRANCH_T_NO_E -> 28.0f;
      case BRANCH_CORNER_NW -> 33.0f;
      case BRANCH_CORNER_NE -> 35.0f;
      case BRANCH_CORNER_SE -> 37.0f;
      case BRANCH_CORNER_SW -> 39.0f;
      case BRANCH_VERTICAL -> 41.0f;
      case BRANCH_HORIZONTAL -> 43.0f;
      default -> 1.0f;
    };
  }

  private float getActiveThreshold(int branchType, int variant) {
    return switch (branchType) {
      case BRANCH_CROSS -> 2.0f + variant;        // 2-12 for variants 0-10
      case BRANCH_T_NO_S -> 14.0f + variant;      // 14-17 for variants 0-3
      case BRANCH_T_NO_W -> 19.0f + variant;      // 19-22 for variants 0-3
      case BRANCH_T_NO_N -> 24.0f + variant;      // 24-27 for variants 0-3
      case BRANCH_T_NO_E -> 29.0f + variant;      // 29-32 for variants 0-3
      case BRANCH_CORNER_NW -> 34.0f;             // Only one active variant
      case BRANCH_CORNER_NE -> 36.0f;
      case BRANCH_CORNER_SE -> 38.0f;
      case BRANCH_CORNER_SW -> 40.0f;
      case BRANCH_VERTICAL -> 42.0f;
      case BRANCH_HORIZONTAL -> 44.0f;
      default -> 2.0f;
    };
  }

  /**
   * Determine the active variant based on which directions are lit.
   * Variant numbering follows the texture naming convention.
   */
  private int getActiveVariant(int branchType, boolean litN, boolean litS, boolean litE, boolean litW) {
    return switch (branchType) {
      case BRANCH_CROSS -> getCrossVariant(litN, litS, litE, litW);
      case BRANCH_T_NO_S -> getTNoSVariant(litN, litE, litW);
      case BRANCH_T_NO_W -> getTNoWVariant(litN, litE, litS);
      case BRANCH_T_NO_N -> getTNoNVariant(litE, litS, litW);
      case BRANCH_T_NO_E -> getTNoEVariant(litN, litS, litW);
      default -> 0; // Corners, vertical, horizontal only have one active variant
    };
  }

  // Cross variants (type 0): N,E,S,W
  // 0=all, 1=no S, 2=no W, 3=no N, 4=no E, 5=no E,S, 6=no W,S, 7=no N,W, 8=no N,E, 9=no E,W, 10=no N,S
  private int getCrossVariant(boolean litN, boolean litS, boolean litE, boolean litW) {
    if (litN && litE && litS && litW) return 0;  // All lit
    if (litN && litE && litW && !litS) return 1;  // No S
    if (litN && litE && litS && !litW) return 2;  // No W
    if (litE && litS && litW && !litN) return 3;  // No N
    if (litN && litS && litW && !litE) return 4;  // No E
    if (litN && litW && !litE && !litS) return 5;  // No E,S
    if (litN && litE && !litW && !litS) return 6;  // No W,S
    if (litE && litS && !litN && !litW) return 7;  // No N,W
    if (litS && litW && !litN && !litE) return 8;  // No N,E
    if (litN && litS && !litE && !litW) return 9;  // No E,W
    if (litE && litW && !litN && !litS) return 10; // No N,S
    return 0; // Default to fully active
  }

  // T no-S variants (type 1): N,E,W
  // 0=all, 1=no E, 2=no W, 3=no N
  private int getTNoSVariant(boolean litN, boolean litE, boolean litW) {
    if (litN && litE && litW) return 0;  // All lit
    if (litN && litW && !litE) return 1;  // No E
    if (litN && litE && !litW) return 2;  // No W
    if (litE && litW && !litN) return 3;  // No N
    return 0;
  }

  // T no-W variants (type 2): N,E,S
  // 0=all, 1=no S, 2=no N, 3=no E
  private int getTNoWVariant(boolean litN, boolean litE, boolean litS) {
    if (litN && litE && litS) return 0;  // All lit
    if (litN && litE && !litS) return 1;  // No S
    if (litE && litS && !litN) return 2;  // No N
    if (litN && litS && !litE) return 3;  // No E
    return 0;
  }

  // T no-N variants (type 3): W,E,S
  // 0=all, 1=no E, 2=no W, 3=no S
  private int getTNoNVariant(boolean litE, boolean litS, boolean litW) {
    if (litW && litE && litS) return 0;  // All lit
    if (litW && litS && !litE) return 1;  // No E
    if (litE && litS && !litW) return 2;  // No W
    if (litW && litE && !litS) return 3;  // No S
    return 0;
  }

  // T no-E variants (type 4): N,W,S
  // 0=all, 1=no S, 2=no N, 3=no W
  private int getTNoEVariant(boolean litN, boolean litS, boolean litW) {
    if (litN && litW && litS) return 0;  // All lit
    if (litN && litW && !litS) return 1;  // No S
    if (litW && litS && !litN) return 2;  // No N
    if (litN && litS && !litW) return 3;  // No W
    return 0;
  }


  /**
   * Creates path segments between two points (horizontal first, then vertical).
   */
  private List<GridPoint> createWynnPath(int x1, int y1, int x2, int y2) {
    List<GridPoint> path = new ArrayList<>();

    // Start point
    path.add(new GridPoint(x1, y1));

    // Horizontal segment from x1 to x2 at y1 (only if x changes)
    if (x1 != x2) {
      int xDir = x2 > x1 ? 1 : -1;
      for (int x = x1 + xDir; x != x2; x += xDir) {
        path.add(new GridPoint(x, y1));
      }
      // Corner point at (x2, y1)
      path.add(new GridPoint(x2, y1));
    }

    // Vertical segment from y1 to y2 at x2 (only if y changes)
    if (y1 != y2) {
      int yDir = y2 > y1 ? 1 : -1;
      for (int y = y1 + yDir; y != y2; y += yDir) {
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
   * Detect segment type for display purposes.
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
      return "Corner";
    }

    // Straight segments
    if (dx1 != 0 && dy1 == 0) {
      return "─ Horizontal";
    } else if (dx1 == 0 && dy1 != 0) {
      return "│ Vertical";
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
   * Update navigation arrows and info book in player inventory.
   */
  private void updateNavigationArrows(Inventory gui, GuiSession session, PlayerUpgradeData data, Player player) {
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

    // Up arrow in player inventory
    ItemStack upArrow = new ItemStack(canScrollUp ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta upMeta = upArrow.getItemMeta();
    upMeta.displayName(Component.text(
        canScrollUp ? "▲ Scroll Up" : "▲ At Top",
        canScrollUp ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    upMeta.getPersistentDataContainer().set(
        new NamespacedKey(plugin, "scroll_action"),
        PersistentDataType.STRING,
        "up"
    );
    upArrow.setItemMeta(upMeta);
    player.getInventory().setItem(NAV_UP_SLOT, upArrow);

    // Info book in player inventory (between arrows)
    player.getInventory().setItem(NAV_INFO_SLOT, createInfoItem(session.job, session.tree, data));

    // Down arrow in player inventory
    ItemStack downArrow = new ItemStack(canScrollDown ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta downMeta = downArrow.getItemMeta();
    downMeta.displayName(Component.text(
        canScrollDown ? "▼ Scroll Down" : "▼ At Bottom",
        canScrollDown ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    downMeta.getPersistentDataContainer().set(
        new NamespacedKey(plugin, "scroll_action"),
        PersistentDataType.STRING,
        "down"
    );
    downArrow.setItemMeta(downMeta);
    player.getInventory().setItem(NAV_DOWN_SLOT, downArrow);
  }

  private void fillBackground(Inventory gui) {
    // Clear all slots (they remain uninteractable via the click handler)
    for (int i = 0; i < GUI_SIZE; i++) {
      gui.setItem(i, null);
    }
  }

  private NodeStatus getNodeStatus(UpgradeNode node, Set<String> unlocked, Set<UpgradeNode> available, UpgradeTree tree) {
    String shortKey = getShortKey(node);

    // Check if unlocked
    if (unlocked.contains(shortKey)) {
      return NodeStatus.UNLOCKED;
    }

    // Check if excluded by any already-unlocked node
    if (tree.isExcludedByUnlockedNode(shortKey, unlocked)) {
      return NodeStatus.EXCLUDED;
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
    boolean unlocked = status == NodeStatus.UNLOCKED;
    String nodeKey = getShortKey(node);
    int currentLevel = data.getNodeLevel(nodeKey);

    // Check if node has custom texture - use KNOWLEDGE_BOOK with CustomModelData
    String nodeTexture = node.nodeTexture();
    if (nodeTexture != null && NODE_TEXTURE_BASE.containsKey(nodeTexture)) {
      return createTexturedNodeItem(node, status, data, tree, nodeTexture);
    }

    // Fallback to material-based icons
    Material material = switch (status) {
      case UNLOCKED -> {
        if (node.isUpgradeable() && currentLevel > 0) {
          yield node.getIconForLevel(currentLevel);
        } else {
          yield node.unlockedIcon();
        }
      }
      case AVAILABLE -> node.icon();
      case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
      case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
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

    // Description - use per-level description for unlocked upgradeable nodes
    String displayDescription;
    if (node.isUpgradeable() && status == NodeStatus.UNLOCKED && currentLevel > 0) {
      displayDescription = node.getDescriptionForLevel(currentLevel);
    } else if (node.isUpgradeable() && status == NodeStatus.AVAILABLE) {
      displayDescription = node.getDescriptionForLevel(1);
    } else {
      displayDescription = node.description();
    }

    if (displayDescription != null && !displayDescription.isEmpty()) {
      for (String line : displayDescription.split("\n")) {
        lore.add(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    lore.add(Component.empty());

    // Show level for upgradeable nodes
    if (node.isUpgradeable()) {
      int maxLevel = node.maxLevel();
      lore.add(Component.text()
          .append(Component.text("Level: ", NamedTextColor.GRAY))
          .append(Component.text(currentLevel + "/" + maxLevel, NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .build());
    }

    // Cost - show next upgrade cost for unlocked upgradeable nodes
    int displayCost;
    if (node.isUpgradeable()) {
      if (status == NodeStatus.UNLOCKED && currentLevel > 0 && currentLevel < node.maxLevel()) {
        // Show cost for next level
        displayCost = node.getCostForLevel(currentLevel + 1);
      } else if (status == NodeStatus.UNLOCKED && currentLevel >= node.maxLevel()) {
        // At max level, show 0 or skip cost line
        displayCost = 0;
      } else {
        // Not unlocked yet, show level 1 cost
        displayCost = node.getCostForLevel(1);
      }
    } else {
      displayCost = node.cost();
    }

    if (displayCost > 0 || !node.isUpgradeable() || status != NodeStatus.UNLOCKED) {
      lore.add(Component.text()
          .append(Component.text("Cost: ", NamedTextColor.GRAY))
          .append(Component.text(displayCost + " SP", NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .build());
    }

    // Effects - use per-level effects for unlocked upgradeable nodes
    List<UpgradeEffect> displayEffects;
    if (node.isUpgradeable() && status == NodeStatus.UNLOCKED && currentLevel > 0) {
      displayEffects = node.getEffectsForLevel(currentLevel);
    } else if (node.isUpgradeable() && status == NodeStatus.AVAILABLE) {
      displayEffects = node.getEffectsForLevel(1);
    } else {
      displayEffects = node.effects();
    }

    if (!displayEffects.isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Effects:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      for (UpgradeEffect effect : displayEffects) {
        lore.add(Component.text("  \u2022 " + formatEffect(effect), NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    // Prerequisites for locked nodes
    if (status == NodeStatus.LOCKED) {
      boolean hasAndPrereqs = !node.prerequisites().isEmpty();
      boolean hasOrPrereqs = !node.prerequisitesOr().isEmpty();

      if (hasAndPrereqs || hasOrPrereqs) {
        lore.add(Component.empty());
      }

      // AND prerequisites (all required)
      if (hasAndPrereqs) {
        lore.add(Component.text("Requires (all):", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        for (String prereq : node.prerequisites()) {
          lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false));
        }
      }

      // OR prerequisites (any one required)
      if (hasOrPrereqs) {
        lore.add(Component.text("Requires (any one):", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        for (String prereq : node.prerequisitesOr()) {
          lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false));
        }
      }
    }

    lore.add(Component.empty());

    // Action hint
    Component actionHint = switch (status) {
      case UNLOCKED -> {
        if (node.isUpgradeable()) {
          if (currentLevel < node.maxLevel()) {
            int nextLevel = currentLevel + 1;
            int upgradeCost = node.getCostForLevel(nextLevel);
            if (data.availableSkillPoints() >= upgradeCost) {
              yield Component.text("Click to upgrade", NamedTextColor.YELLOW);
            } else {
              yield Component.text("Not enough SP to upgrade", NamedTextColor.RED);
            }
          } else {
            yield Component.text("\u2714 Max Level!", NamedTextColor.GREEN);
          }
        } else {
          yield Component.text("\u2714 Unlocked!", NamedTextColor.GREEN);
        }
      }
      case AVAILABLE -> {
        int unlockCost = node.isUpgradeable() ? node.getCostForLevel(1) : node.cost();
        if (data.availableSkillPoints() >= unlockCost) {
          if (node.isUpgradeable()) {
            yield Component.text("Click to unlock (upgradeable to Lv." + node.maxLevel() + ")", NamedTextColor.YELLOW);
          } else {
            yield Component.text("Click to unlock", NamedTextColor.YELLOW);
          }
        } else {
          yield Component.text("Not enough SP!", NamedTextColor.RED);
        }
      }
      case LOCKED -> Component.text("Locked", NamedTextColor.DARK_GRAY);
      case EXCLUDED -> Component.text("\u2718 Path Locked (Exclusive Choice)", NamedTextColor.RED);
    };
    lore.add(actionHint.decoration(TextDecoration.ITALIC, false));

    // Show which exclusive node locked this path
    if (status == NodeStatus.EXCLUDED) {
      Set<String> excludingNodeKeys = tree.getExcludingNodes(getShortKey(node), data.unlockedNodes());
      if (!excludingNodeKeys.isEmpty()) {
        List<String> exclusiveNames = excludingNodeKeys.stream()
            .map(tree::getNode)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(UpgradeNode::name)
            .toList();
        lore.add(Component.text("Blocked by: " + String.join(", ", exclusiveNames), NamedTextColor.DARK_RED)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    meta.lore(lore);

    // Set custom item model if available (for UNLOCKED, AVAILABLE, and LOCKED nodes)
    String itemModel = node.getItemModelForState(unlocked);
    if (itemModel != null) {
      NamespacedKey modelKey = NamespacedKey.fromString(itemModel);
      if (modelKey != null) {
        meta.setItemModel(modelKey);
      }
    }

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

  /**
   * Create a node item using KNOWLEDGE_BOOK with custom textures via CustomModelData.
   * Supports: locked, available (with pulse overlay), active, and fragment states.
   */
  private ItemStack createTexturedNodeItem(UpgradeNode node, NodeStatus status, PlayerUpgradeData data,
                                           UpgradeTree tree, String nodeTexture) {
    String nodeKey = getShortKey(node);
    int currentLevel = data.getNodeLevel(nodeKey);

    ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
    ItemMeta meta = item.getItemMeta();

    // Determine state offset based on node status
    int stateOffset = switch (status) {
      case LOCKED -> NODE_STATE_LOCKED;
      case AVAILABLE -> NODE_STATE_AVAILABLE;  // Uses pulse overlay
      case UNLOCKED -> NODE_STATE_ACTIVE;
      case EXCLUDED -> NODE_STATE_FRAGMENT;
    };

    // Calculate CustomModelData threshold: colorBase + stateOffset
    float baseThreshold = NODE_TEXTURE_BASE.get(nodeTexture);
    float customModelDataValue = baseThreshold + stateOffset;

    // Set display name with status color
    NamedTextColor nameColor = switch (status) {
      case UNLOCKED -> NamedTextColor.GREEN;
      case AVAILABLE -> NamedTextColor.YELLOW;
      case LOCKED -> NamedTextColor.GRAY;
      case EXCLUDED -> NamedTextColor.RED;
    };

    meta.displayName(Component.text(node.name(), nameColor)
        .decoration(TextDecoration.ITALIC, false));

    // Build lore (same logic as material-based items)
    List<Component> lore = new ArrayList<>();

    // Description - use per-level description for unlocked upgradeable nodes
    String displayDescription;
    if (node.isUpgradeable() && status == NodeStatus.UNLOCKED && currentLevel > 0) {
      displayDescription = node.getDescriptionForLevel(currentLevel);
    } else if (node.isUpgradeable() && status == NodeStatus.AVAILABLE) {
      displayDescription = node.getDescriptionForLevel(1);
    } else {
      displayDescription = node.description();
    }

    if (displayDescription != null && !displayDescription.isEmpty()) {
      for (String line : displayDescription.split("\n")) {
        lore.add(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    lore.add(Component.empty());

    // Show level for upgradeable nodes
    if (node.isUpgradeable()) {
      int maxLevel = node.maxLevel();
      lore.add(Component.text()
          .append(Component.text("Level: ", NamedTextColor.GRAY))
          .append(Component.text(currentLevel + "/" + maxLevel, NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .build());
    }

    // Cost
    int displayCost;
    if (node.isUpgradeable()) {
      if (status == NodeStatus.UNLOCKED && currentLevel > 0 && currentLevel < node.maxLevel()) {
        displayCost = node.getCostForLevel(currentLevel + 1);
      } else if (status == NodeStatus.UNLOCKED && currentLevel >= node.maxLevel()) {
        displayCost = 0;
      } else {
        displayCost = node.getCostForLevel(1);
      }
    } else {
      displayCost = node.cost();
    }

    if (displayCost > 0 || !node.isUpgradeable() || status != NodeStatus.UNLOCKED) {
      lore.add(Component.text()
          .append(Component.text("Cost: ", NamedTextColor.GRAY))
          .append(Component.text(displayCost + " SP", NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .build());
    }

    // Effects
    List<UpgradeEffect> displayEffects;
    if (node.isUpgradeable() && status == NodeStatus.UNLOCKED && currentLevel > 0) {
      displayEffects = node.getEffectsForLevel(currentLevel);
    } else if (node.isUpgradeable() && status == NodeStatus.AVAILABLE) {
      displayEffects = node.getEffectsForLevel(1);
    } else {
      displayEffects = node.effects();
    }

    if (!displayEffects.isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Effects:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      for (UpgradeEffect effect : displayEffects) {
        lore.add(Component.text("  \u2022 " + formatEffect(effect), NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    // Prerequisites for locked nodes
    if (status == NodeStatus.LOCKED) {
      boolean hasAndPrereqs = !node.prerequisites().isEmpty();
      boolean hasOrPrereqs = !node.prerequisitesOr().isEmpty();

      if (hasAndPrereqs || hasOrPrereqs) {
        lore.add(Component.empty());
      }

      if (hasAndPrereqs) {
        lore.add(Component.text("Requires (all):", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        for (String prereq : node.prerequisites()) {
          lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false));
        }
      }

      if (hasOrPrereqs) {
        lore.add(Component.text("Requires (any one):", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        for (String prereq : node.prerequisitesOr()) {
          lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false));
        }
      }
    }

    lore.add(Component.empty());

    // Action hint
    Component actionHint = switch (status) {
      case UNLOCKED -> {
        if (node.isUpgradeable()) {
          if (currentLevel < node.maxLevel()) {
            int nextLevel = currentLevel + 1;
            int upgradeCost = node.getCostForLevel(nextLevel);
            if (data.availableSkillPoints() >= upgradeCost) {
              yield Component.text("Click to upgrade", NamedTextColor.YELLOW);
            } else {
              yield Component.text("Not enough SP to upgrade", NamedTextColor.RED);
            }
          } else {
            yield Component.text("\u2714 Max Level!", NamedTextColor.GREEN);
          }
        } else {
          yield Component.text("\u2714 Unlocked!", NamedTextColor.GREEN);
        }
      }
      case AVAILABLE -> {
        int unlockCost = node.isUpgradeable() ? node.getCostForLevel(1) : node.cost();
        if (data.availableSkillPoints() >= unlockCost) {
          if (node.isUpgradeable()) {
            yield Component.text("Click to unlock (upgradeable to Lv." + node.maxLevel() + ")", NamedTextColor.YELLOW);
          } else {
            yield Component.text("Click to unlock", NamedTextColor.YELLOW);
          }
        } else {
          yield Component.text("Not enough SP!", NamedTextColor.RED);
        }
      }
      case LOCKED -> Component.text("Locked", NamedTextColor.DARK_GRAY);
      case EXCLUDED -> Component.text("\u2718 Path Locked (Exclusive Choice)", NamedTextColor.RED);
    };
    lore.add(actionHint.decoration(TextDecoration.ITALIC, false));

    // Show which exclusive node locked this path
    if (status == NodeStatus.EXCLUDED) {
      Set<String> excludingNodeKeys = tree.getExcludingNodes(getShortKey(node), data.unlockedNodes());
      if (!excludingNodeKeys.isEmpty()) {
        List<String> exclusiveNames = excludingNodeKeys.stream()
            .map(tree::getNode)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(UpgradeNode::name)
            .toList();
        lore.add(Component.text("Blocked by: " + String.join(", ", exclusiveNames), NamedTextColor.DARK_RED)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    meta.lore(lore);

    // Store node key in PDC
    meta.getPersistentDataContainer().set(nodeKeyTag, PersistentDataType.STRING, getShortKey(node));

    item.setItemMeta(meta);

    // Apply CustomModelData AFTER setItemMeta (setItemMeta overwrites data components)
    item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
        CustomModelData.customModelData().addFloat(customModelDataValue).build());

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

    // Attempt to unlock or upgrade
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();

    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);

    // If already unlocked, try to upgrade instead
    if (result instanceof UnlockResult.AlreadyUnlocked) {
      result = upgradeService.upgradeNode(playerId, jobKey, nodeKey);
    }

    switch (result) {
      case UnlockResult.Success success -> {
        Mint.sendThemedMessage(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        // Refresh the GUI
        refresh(player);
      }
      case UnlockResult.NodeUpgraded upgraded -> {
        Mint.sendThemedMessage(player, "<accent>Upgraded to level <primary>" + upgraded.newLevel() + "/" + upgraded.maxLevel()
            + " <neutral>(<secondary>" + upgraded.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        // Refresh the GUI
        refresh(player);
      }
      case UnlockResult.AlreadyMaxLevel maxLvl -> {
        Mint.sendThemedMessage(player, "<warning>Already at max level!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
      }
      case UnlockResult.NodeNotUnlocked notUnlocked -> {
        Mint.sendThemedMessage(player, "<error>Node not unlocked yet!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
      case UnlockResult.OrPrerequisitesNotMet opm -> {
        Mint.sendThemedMessage(player, "<error>Need one of: <secondary>" + String.join(", ", opm.options()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Mint.sendThemedMessage(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.AlreadyUnlocked au -> {
        // This shouldn't happen anymore since we try upgrade, but keep as fallback
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

    // Update scroll offset by full page
    if ("up".equals(action) && session.scrollOffset > 0) {
      session.scrollOffset = Math.max(0, session.scrollOffset - GUI_ROWS);
    } else if ("down".equals(action) && session.scrollOffset < maxScroll) {
      session.scrollOffset = Math.min(maxScroll, session.scrollOffset + GUI_ROWS);
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
      GuiSession session = openGuis.remove(playerId);

      // Restore player's entire inventory
      if (session != null && session.savedInventory != null) {
        player.getInventory().setContents(session.savedInventory);
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    GuiSession session = openGuis.remove(playerId);

    // Restore inventory on quit (in case GUI was open)
    if (session != null && session.savedInventory != null) {
      event.getPlayer().getInventory().setContents(session.savedInventory);
    }
  }

  private enum NodeStatus {
    UNLOCKED, AVAILABLE, LOCKED, EXCLUDED
  }
}
