package net.aincraft.gui;

import net.aincraft.util.Messages;
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
import net.aincraft.upgrade.NodeEffect;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.SkillNode;
import net.aincraft.upgrade.SkillTree;
import net.aincraft.upgrade.SkillTreeState;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.PurchaseResult;
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

  private static final int GUI_SIZE = 54; // 6 rows total
  private static final int GUI_ROWS = 5; // Rows for node rendering (bottom row reserved for controls)
  private static final int GUI_COLS = 9;
  private static final int CONTROL_ROW_START = 45; // First slot of bottom control row (row 5)
  private static final int CONFIRM_SLOT = 52; // Major purchase confirmation slot in the control row

  private final Plugin plugin;
  private final UpgradeService upgradeService;
  private final NamespacedKey nodeKeyTag;
  private final NamespacedKey confirmTag;

  // Track open GUIs: player UUID -> session data
  private final Map<UUID, GuiSession> openGuis = new HashMap<>();

  private static class GuiSession {
    final Job job;
    final UpgradeTree tree;   // legacy tree; null for v2-only jobs
    final SkillTree skillTree; // v2 tree; null for legacy-only jobs
    final Inventory gui;      // the inventory this session renders into
    String pendingMajorKey;   // set when a major awaits confirmation, null otherwise
    int scrollOffset;

    GuiSession(Job job, UpgradeTree tree, SkillTree skillTree, Inventory gui) {
      this.job = job;
      this.tree = tree;
      this.skillTree = skillTree;
      this.gui = gui;
      this.pendingMajorKey = null;
      this.scrollOffset = 0;
    }

    boolean isV2() {
      return skillTree != null;
    }
  }

  public UpgradeTreeGui(Plugin plugin, UpgradeService upgradeService) {
    this.plugin = plugin;
    this.upgradeService = upgradeService;
    this.nodeKeyTag = new NamespacedKey(plugin, "upgrade_node");
    this.confirmTag = new NamespacedKey(plugin, "gui_action");
  }

  /**
   * Open the upgrade tree GUI for a player. The v2 tree for the job is
   * resolved from the service; legacy and v2 render paths share this entry.
   *
   * @param tree the legacy tree, or null when the job has a v2 tree only
   */
  public void open(Player player, Job job, UpgradeTree tree) {
    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    SkillTree skillTree = upgradeService.getSkillTree(jobKey).orElse(null);
    if (tree == null && skillTree == null) {
      return; // No tree of any kind for this job
    }

    Component title = Component.text()
        .append(job.displayName())
        .append(Component.text(" Upgrades", NamedTextColor.GRAY))
        .build();

    Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

    // Create and store session
    UUID playerUuid = player.getUniqueId();
    GuiSession session = new GuiSession(job, tree, skillTree, gui);
    openGuis.put(playerUuid, session);

    // Fill background with glass panes
    fillBackground(gui);

    // Place nodes based on their positions (with scroll offset)
    renderNodes(gui, session, loadData(playerId, jobKey, session), loadState(playerId, jobKey, session));

    // Add navigation arrows and info book to control row
    updateNavigationArrows(gui, session, loadData(playerId, jobKey, session), loadState(playerId, jobKey, session));

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

    String id = player.getUniqueId().toString();
    String jobKey = session.job.key().value();

    // Re-render nodes with current scroll offset
    fillBackground(gui);
    renderNodes(gui, session, loadData(id, jobKey, session), loadState(id, jobKey, session));

    // Update navigation arrows and info book in control row
    updateNavigationArrows(gui, session, loadData(id, jobKey, session), loadState(id, jobKey, session));
  }

  private PlayerUpgradeData loadData(String playerId, String jobKey, GuiSession session) {
    return session.isV2() ? null : upgradeService.getPlayerData(playerId, jobKey);
  }

  private SkillTreeState loadState(String playerId, String jobKey, GuiSession session) {
    return session.isV2() ? upgradeService.getSkillTreeState(playerId, jobKey) : null;
  }

  /**
   * Render nodes in the GUI with the current scroll offset.
   */
  private void renderNodes(Inventory gui, GuiSession session, PlayerUpgradeData data, SkillTreeState state) {
    if (session.isV2()) {
      renderV2Nodes(gui, session, state);
      return;
    }
    Set<String> unlocked = data.unlockedNodes();
    Set<UpgradeNode> available = session.tree.getAvailableNodes(unlocked, data);

    // First, render connection lines between nodes
    renderConnections(gui, session, unlocked, available);

    // Then render ability nodes
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

  /** Version-2 render path: nodes iterate the SkillTree, status from node levels. */
  private void renderV2Nodes(Inventory gui, GuiSession session, SkillTreeState state) {
    for (SkillNode node : session.skillTree.nodes()) {
      int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
      if (slot < 0 || slot >= GUI_SIZE) {
        continue; // Outside visible area
      }
      NodeStatus status = v2Status(session.skillTree, state, node);
      gui.setItem(slot, createV2NodeItem(node, status, state, session.skillTree));
    }
  }

  private NodeStatus v2Status(SkillTree tree, SkillTreeState state, SkillNode node) {
    String key = node.key().value();
    if (state.levelOf(key) > 0) {
      return NodeStatus.UNLOCKED;
    }
    if (tree.canPurchase(state, key)) {
      return NodeStatus.AVAILABLE;
    }
    boolean excluded = tree.symmetricExcludes(key).stream().anyMatch(state::hasUnlocked);
    return excluded ? NodeStatus.EXCLUDED : NodeStatus.LOCKED;
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
    for (GridPoint pathPoint : allPathPoints) {
      int screenY = pathPoint.y - scrollOffset;
      // Only render if visible on current page
      if (screenY < 0 || screenY >= GUI_ROWS || pathPoint.x < 0 || pathPoint.x >= GUI_COLS) {
        continue;
      }

      boolean isLit = litPathPoints.contains(pathPoint);
      Material material = isLit ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;

      ItemStack lineItem = new ItemStack(material);
      ItemMeta meta = lineItem.getItemMeta();
      meta.displayName(Component.text(" "));
      lineItem.setItemMeta(meta);

      int slot = screenY * GUI_COLS + pathPoint.x;
      if (slot >= 0 && slot < GUI_SIZE) {
        gui.setItem(slot, lineItem);
      }
    }
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
   * Update navigation arrows and info book in the GUI bottom control row.
   */
  private void updateNavigationArrows(Inventory gui, GuiSession session, PlayerUpgradeData data, SkillTreeState state) {
    // Calculate max scroll based on tree bounds
    int maxY = session.isV2()
        ? session.skillTree.nodes().stream()
            .map(SkillNode::position)
            .filter(pos -> pos != null)
            .mapToInt(Position::y)
            .max()
            .orElse(0)
        : session.tree.allNodes().stream()
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

    // Fill control row background
    for (int i = CONTROL_ROW_START; i < GUI_SIZE; i++) {
      ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta paneMeta = pane.getItemMeta();
      paneMeta.displayName(Component.text(" "));
      pane.setItemMeta(paneMeta);
      gui.setItem(i, pane);
    }

    // Up arrow in control row slot 0 (GUI slot 45)
    ItemStack upArrow = new ItemStack(canScrollUp ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta upMeta = upArrow.getItemMeta();
    upMeta.displayName(Component.text(
        canScrollUp ? "Scroll Up" : "Scroll Up (At Top)",
        canScrollUp ? NamedTextColor.AQUA : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    upMeta.getPersistentDataContainer().set(
        new NamespacedKey(plugin, "scroll_action"),
        PersistentDataType.STRING,
        "up"
    );
    upArrow.setItemMeta(upMeta);
    gui.setItem(CONTROL_ROW_START, upArrow);

    // Down arrow in control row slot 8 (GUI slot 53)
    ItemStack downArrow = new ItemStack(canScrollDown ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta downMeta = downArrow.getItemMeta();
    downMeta.displayName(Component.text(
        canScrollDown ? "Scroll Down" : "Scroll Down (At Bottom)",
        canScrollDown ? NamedTextColor.AQUA : NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    downMeta.getPersistentDataContainer().set(
        new NamespacedKey(plugin, "scroll_action"),
        PersistentDataType.STRING,
        "down"
    );
    downArrow.setItemMeta(downMeta);
    gui.setItem(CONTROL_ROW_START + 8, downArrow);

    // Info book in control row center slot (GUI slot 49)
    gui.setItem(CONTROL_ROW_START + 4, session.isV2()
        ? createV2InfoItem(session.skillTree, state)
        : createInfoItem(session.job, session.tree, data));

    // Major confirmation slot (GUI slot 52) only while a major awaits confirmation
    String pending = session.pendingMajorKey;
    if (pending != null) {
      ItemStack confirm = new ItemStack(Material.GOLD_INGOT);
      ItemMeta confirmMeta = confirm.getItemMeta();
      String pendingName = session.skillTree.node(pending).map(SkillNode::name).orElse(pending);
      confirmMeta.displayName(Component.text("\u2753 Confirm Major?", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      confirmMeta.lore(List.of(
          Component.text(pendingName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
          Component.text("Permanent choice - cannot be refunded", NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false),
          Component.text("Click to confirm", NamedTextColor.YELLOW)
              .decoration(TextDecoration.ITALIC, false)));
      confirmMeta.getPersistentDataContainer().set(confirmTag, PersistentDataType.STRING, "confirm_major");
      confirm.setItemMeta(confirmMeta);
      gui.setItem(CONFIRM_SLOT, confirm);
    }
  }

  private void fillBackground(Inventory gui) {
    ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(" "));
    pane.setItemMeta(meta);

    // Only fill node rendering area (rows 0-4), control row is handled separately
    for (int i = 0; i < CONTROL_ROW_START; i++) {
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
    boolean unlocked = status == NodeStatus.UNLOCKED;
    Material material = switch (status) {
      case UNLOCKED -> materialFromName(node.unlockedIcon());
      case AVAILABLE -> materialFromName(node.icon());
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
      for (String line : node.description().split("\n")) {
        lore.add(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
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

    // Prerequisites for locked nodes (just unlocked)
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Requires:", NamedTextColor.RED)
          .decoration(TextDecoration.ITALIC, false));
      for (String prereq : node.prerequisites()) {
        lore.add(Component.text("  \u2022 " + prereq, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    // Maxed prerequisites for locked nodes (must be at MAX level)
    if (status == NodeStatus.LOCKED && !node.maxedPrerequisites().isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Requires MAX level:", NamedTextColor.RED)
          .decoration(TextDecoration.ITALIC, false));
      for (String prereq : node.maxedPrerequisites()) {
        lore.add(Component.text("  \u2022 " + prereq + " [MAX]", NamedTextColor.GOLD)
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

  private ItemStack createV2NodeItem(SkillNode node, NodeStatus status, SkillTreeState state, SkillTree tree) {
    String key = node.key().value();
    int owned = state.levelOf(key);
    boolean unlocked = status == NodeStatus.UNLOCKED;
    Material material = switch (status) {
      case UNLOCKED -> materialFromName(node.unlockedIcon());
      case AVAILABLE -> materialFromName(node.lockedIcon());
      case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
      case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
    };

    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    NamedTextColor nameColor = switch (status) {
      case UNLOCKED -> NamedTextColor.GREEN;
      case AVAILABLE -> NamedTextColor.YELLOW;
      case LOCKED -> NamedTextColor.GRAY;
      case EXCLUDED -> NamedTextColor.RED;
    };
    meta.displayName(Component.text(node.name(), nameColor)
        .decoration(TextDecoration.ITALIC, false));

    List<Component> lore = new ArrayList<>();
    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
      lore.add(Component.empty());
    }

    if (node.isSkill()) {
      lore.add(Component.text()
          .append(Component.text("Level: ", NamedTextColor.GRAY))
          .append(Component.text(owned + "/" + node.maxLevel(), NamedTextColor.GOLD))
          .decoration(TextDecoration.ITALIC, false)
          .build());
    } else {
      lore.add(Component.text("Permanent choice", NamedTextColor.LIGHT_PURPLE)
          .decoration(TextDecoration.ITALIC, false));
    }

    int cost = node.isSkill() ? node.levelCost(owned + 1) : node.cost();
    lore.add(Component.text()
        .append(Component.text("Cost: ", NamedTextColor.GRAY))
        .append(Component.text(cost + " SP", NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .build());

    // Effects currently active (level badges for skills)
    if (status == NodeStatus.UNLOCKED && !node.activeEffects(owned).isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text("Active Effects:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      for (NodeEffect effect : node.activeEffects(owned)) {
        lore.add(Component.text("  \u2022 " + formatV2Effect(effect), NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

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

    Component actionHint;
    if (status == NodeStatus.UNLOCKED) {
      actionHint = Component.text("\u2714 Unlocked!", NamedTextColor.GREEN);
    } else if (status == NodeStatus.AVAILABLE) {
      if (node.isMajor()) {
        actionHint = Component.text("\u2753 Click to confirm - permanent choice", NamedTextColor.GOLD);
      } else if (cost > tree.availablePoints(state)) {
        actionHint = Component.text("Not enough SP!", NamedTextColor.RED);
      } else {
        actionHint = Component.text("Click to unlock", NamedTextColor.YELLOW);
      }
    } else if (status == NodeStatus.EXCLUDED) {
      actionHint = Component.text("\u2718 Path Locked (Exclusive Choice)", NamedTextColor.RED);
    } else {
      actionHint = Component.text("Locked", NamedTextColor.DARK_GRAY);
    }
    lore.add(actionHint.decoration(TextDecoration.ITALIC, false));

    meta.lore(lore);

    if (status == NodeStatus.UNLOCKED || status == NodeStatus.AVAILABLE) {
      meta.addEnchant(Enchantment.UNBREAKING, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    meta.getPersistentDataContainer().set(nodeKeyTag, PersistentDataType.STRING, key);
    item.setItemMeta(meta);
    return item;
  }

  private ItemStack createV2InfoItem(SkillTree tree, SkillTreeState state) {
    ItemStack item = new ItemStack(Material.BOOK);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(Component.text("Skill Tree Info", NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false));

    long unlockedCount = state.nodeLevels().values().stream().filter(l -> l > 0).count();
    List<Component> lore = new ArrayList<>();
    lore.add(Component.text()
        .append(Component.text("Available SP: ", NamedTextColor.GRAY))
        .append(Component.text(tree.availablePoints(state), NamedTextColor.GREEN))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.text()
        .append(Component.text("Total SP: ", NamedTextColor.GRAY))
        .append(Component.text(state.totalSkillPoints(), NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.text()
        .append(Component.text("Unlocked: ", NamedTextColor.GRAY))
        .append(Component.text(unlockedCount + "/" + tree.nodes().size(), NamedTextColor.YELLOW))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.empty());
    lore.add(Component.text()
        .append(Component.text("Job Level: ", NamedTextColor.GRAY))
        .append(Component.text(state.jobLevel(), NamedTextColor.WHITE))
        .decoration(TextDecoration.ITALIC, false)
        .build());
    lore.add(Component.text()
        .append(Component.text("SP per level: ", NamedTextColor.GRAY))
        .append(Component.text(tree.skillPointsPerLevel(), NamedTextColor.WHITE))
        .decoration(TextDecoration.ITALIC, false)
        .build());

    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  private String formatV2Effect(NodeEffect effect) {
    return switch (effect) {
      case NodeEffect.BoostEffect boost ->
          String.format("+%.0f%% %s", (boost.multiplier().doubleValue() - 1) * 100, boost.target());
      case NodeEffect.RuledBoostEffect ruled -> {
        String desc = ruled.boostSource().description();
        yield desc != null ? desc : String.format("Conditional %s boost", ruled.target());
      }
      case NodeEffect.PermissionEffect perm ->
          String.format("Permission: %s", String.join(", ", perm.permissions()));
      case NodeEffect.RecipeUnlockEffect recipe ->
          String.format("Recipe: %s", recipe.recipeKey().asString());
      case NodeEffect.StateSetEffect stateSet ->
          stateSet.remove()
              ? String.format("Removes %s", stateSet.key().asString())
              : String.format("Sets %s = %s", stateSet.key().asString(), stateSet.value());
    };
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

    // Confirm button for a pending major purchase
    String confirmAction = meta.getPersistentDataContainer().get(confirmTag, PersistentDataType.STRING);
    if ("confirm_major".equals(confirmAction)) {
      if (session.pendingMajorKey != null) {
        purchaseMajor(player, session, session.pendingMajorKey);
      }
      return;
    }

    // Check for node click
    String nodeKey = meta.getPersistentDataContainer().get(nodeKeyTag, PersistentDataType.STRING);
    if (nodeKey == null) {
      return; // Clicked on non-node item (background, info)
    }

    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();

    if (session.isV2()) {
      handleV2NodeClick(player, session, nodeKey, playerId, jobKey);
      return;
    }

    handleLegacyUnlock(player, session, nodeKey, playerId, jobKey);
  }

  /** Version-2 click routing: majors stage a confirmation, skills purchase directly. */
  private void handleV2NodeClick(Player player, GuiSession session, String nodeKey, String playerId, String jobKey) {
    SkillNode node = session.skillTree.node(nodeKey).orElse(null);
    if (node != null && node.isMajor()) {
      // Any click while a different major is pending cancels that intent.
      if (session.pendingMajorKey != null && !session.pendingMajorKey.equals(nodeKey)) {
        session.pendingMajorKey = null;
        refresh(player);
      }
      if (session.pendingMajorKey == null) {
        SkillTreeState state = upgradeService.getSkillTreeState(playerId, jobKey);
        if (session.skillTree.canPurchase(state, nodeKey)) {
          // Stage the confirmation; only the CONFIRM slot invokes the purchase.
          session.pendingMajorKey = nodeKey;
          refresh(player);
        } else if (state.levelOf(nodeKey) > 0) {
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
        } else {
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
      }
      return;
    }

    // Any other node click cancels a pending major confirmation.
    if (session.pendingMajorKey != null) {
      session.pendingMajorKey = null;
      refresh(player);
    }

    PurchaseResult result = upgradeService.purchaseSkillLevel(playerId, jobKey, nodeKey);
    handleSkillPurchase(player, session, result);
  }

  private void purchaseMajor(Player player, GuiSession session, String nodeKey) {
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();
    PurchaseResult result = upgradeService.purchaseMajor(playerId, jobKey, nodeKey);
    session.pendingMajorKey = null;

    switch (result) {
      case PurchaseResult.Success success -> {
        Messages.send(player, "<accent>Chosen: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        refresh(player);
      }
      case PurchaseResult.AlreadyOwned ao ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      case PurchaseResult.ExcludedByChoice ec -> {
        Messages.send(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.RequirementsNotMet rn -> {
        Messages.send(player, "<error>Requirements not met.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.PrerequisitesNotMet pn -> {
        Messages.send(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pn.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.InsufficientPoints ip -> {
        Messages.send(player, "<error>Not enough SP! Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case PurchaseResult.TreeNotFound tf -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
    if (!(result instanceof PurchaseResult.Success)) {
      refresh(player); // Drop the confirm slot on any failure
    }
  }

  private void handleSkillPurchase(Player player, GuiSession session, PurchaseResult result) {
    switch (result) {
      case PurchaseResult.Success success -> {
        Messages.send(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        refresh(player);
      }
      case PurchaseResult.AlreadyOwned ao ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      case PurchaseResult.ExcludedByChoice ec -> {
        Messages.send(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.RequirementsNotMet rn -> {
        Messages.send(player, "<error>Requirements not met.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.PrerequisitesNotMet pn -> {
        Messages.send(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pn.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.InsufficientPoints ip -> {
        Messages.send(player, "<error>Not enough SP! Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case PurchaseResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case PurchaseResult.TreeNotFound tf -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
  }

  private void handleLegacyUnlock(Player player, GuiSession session, String nodeKey, String playerId, String jobKey) {
    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);

    switch (result) {
      case UnlockResult.Success success -> {
        Messages.send(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        // Refresh the GUI
        refresh(player);
      }
      case UnlockResult.InsufficientPoints ip -> {
        Messages.send(player, "<error>Not enough SP! Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.PrerequisitesNotMet pm -> {
        Messages.send(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pm.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Messages.send(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.AlreadyUnlocked au -> {
        // Already unlocked - play a subtle click sound
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
      }
      case UnlockResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case UnlockResult.TreeNotFound tf -> {
        Messages.send(player, "<error>No upgrade tree for this job.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }
  }

  /**
   * Handle scroll action from navigation arrows.
   */
  private void handleScroll(Player player, GuiSession session, String action) {
    // Calculate max scroll
    int maxY = session.isV2()
        ? session.skillTree.nodes().stream()
            .map(SkillNode::position)
            .filter(pos -> pos != null)
            .mapToInt(Position::y)
            .max()
            .orElse(0)
        : session.tree.allNodes().stream()
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
      // Re-opening the GUI replaces the session BEFORE the old inventory's
      // close event fires; only remove the session when the closing inventory
      // is the one this session renders into.
      GuiSession session = openGuis.get(playerId);
      if (session != null && session.gui == event.getInventory()) {
        openGuis.remove(playerId);
      }
    }
  }

  private enum NodeStatus {
    UNLOCKED, AVAILABLE, LOCKED, EXCLUDED
  }

  private static Material materialFromName(String name) {
    if (name == null || name.isBlank()) {
      return Material.BARRIER;
    }
    String bare = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
    try {
      return Material.valueOf(bare.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return Material.BARRIER;
    }
  }
}
