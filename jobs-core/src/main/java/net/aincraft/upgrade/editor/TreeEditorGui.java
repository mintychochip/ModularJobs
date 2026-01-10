package net.aincraft.upgrade.editor;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.config.UpgradeTreeLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main GUI for editing upgrade trees using Triumph GUI.
 * Provides a visual canvas with drag-and-drop node placement.
 */
public final class TreeEditorGui {

  private static final int GUI_SIZE = 54; // 6 rows
  private static final int GUI_ROWS = 6;
  private static final int GUI_COLS = 9;
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  // All 6 rows are canvas - controls go in player hotbar
  private static final int CANVAS_START_ROW = 0;
  private static final int CANVAS_ROWS = 6;

  private final Plugin plugin;
  private final TreeEditorExporter exporter;
  private final UpgradeTreeLoader treeLoader;

  // Injected lazily to avoid circular dependency
  @Inject
  private TreeEditorNodeGui nodeEditorGui;

  @Inject
  private TreeEditorSettingsGui settingsGui;

  // Active sessions: player UUID -> session
  private final Map<UUID, EditorSession> sessions = new HashMap<>();

  // Store player inventories while editing
  private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

  // Store open GUIs for refresh
  private final Map<UUID, Gui> openGuis = new HashMap<>();

  // Track players transitioning to sub-GUIs (prevents session cleanup)
  private final java.util.Set<UUID> transitioningToSubGui = new java.util.HashSet<>();

  @Inject
  public TreeEditorGui(Plugin plugin, TreeEditorExporter exporter, UpgradeTreeLoader treeLoader) {
    this.plugin = plugin;
    this.exporter = exporter;
    this.treeLoader = treeLoader;
  }

  /**
   * Open editor for an existing upgrade tree.
   */
  public void open(@NotNull Player player, @NotNull UpgradeTree tree) {
    EditorTree editorTree = EditorTree.fromUpgradeTree(tree);
    openEditor(player, editorTree);
  }

  /**
   * Open editor for a new blank tree.
   */
  public void openNew(@NotNull Player player, @NotNull String jobKey) {
    EditorTree editorTree = EditorTree.createBlank(jobKey);
    openEditor(player, editorTree);
  }

  private void openEditor(@NotNull Player player, @NotNull EditorTree tree) {
    UUID playerId = player.getUniqueId();

    // Create session
    EditorSession session = new EditorSession(playerId, tree);
    sessions.put(playerId, session);

    // Save player inventory and set up hotbar controls
    savedInventories.put(playerId, player.getInventory().getContents().clone());
    player.getInventory().clear();
    setupHotbarControls(player, session);

    // Create and open GUI
    Gui gui = createGui(player, session);
    gui.open(player);
    openGuis.put(playerId, gui);
  }

  /**
   * Refresh the GUI for a player.
   */
  public void refresh(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    EditorSession session = sessions.get(playerId);
    if (session == null) return;

    // Update hotbar controls
    setupHotbarControls(player, session);

    // Re-render the GUI
    Gui gui = openGuis.get(playerId);
    if (gui != null) {
      renderCanvas(gui, player, session);
      gui.update();
    }
  }

  /**
   * Get the current session for a player.
   */
  public Optional<EditorSession> getSession(@NotNull Player player) {
    return Optional.ofNullable(sessions.get(player.getUniqueId()));
  }

  // ========== GUI Creation ==========

  private Gui createGui(Player player, EditorSession session) {
    Component title = Component.text()
        .append(Component.text("Tree Editor: ", NamedTextColor.DARK_GRAY))
        .append(Component.text(session.tree().displayName(), NamedTextColor.GOLD))
        .build();

    Gui gui = Gui.gui()
        .title(title)
        .rows(GUI_ROWS)
        .create();

    // Cancel all clicks on GUI by default
    gui.setDefaultClickAction(event -> {
      event.setCancelled(true);
    });

    // Handle clicks on player inventory (hotbar controls)
    gui.setPlayerInventoryAction(event -> {
      event.setCancelled(true);
      handleHotbarClick(player, session, event);
    });

    // Set close handler
    gui.setCloseGuiAction(event -> {
      Player p = (Player) event.getPlayer();
      handleClose(p);
    });

    // Render canvas
    renderCanvas(gui, player, session);

    return gui;
  }

  private void renderCanvas(Gui gui, Player player, EditorSession session) {
    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();

    // Fill background with clickable panes for each slot
    for (int i = 0; i < GUI_SIZE; i++) {
      final int slot = i;
      GuiItem pane = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
          .setName(" ")
          .asGuiItem(event -> {
            event.setCancelled(true);
            handleEmptySlotClick(player, session, slot);
          });
      gui.setItem(i, pane);
    }

    // First pass: render connection lines
    renderConnections(gui, player, session);

    // Second pass: render nodes
    for (EditorNode node : tree.nodes().values()) {
      Position pos = node.position();
      if (pos == null) continue;

      int canvasX = pos.x() - scrollX;
      int canvasY = pos.y() - scrollY;

      // Check if visible in canvas area
      if (canvasX < 0 || canvasX >= GUI_COLS || canvasY < 0 || canvasY >= CANVAS_ROWS) {
        continue;
      }

      int slot = (canvasY + CANVAS_START_ROW) * GUI_COLS + canvasX;
      if (slot >= 0 && slot < GUI_SIZE) {
        gui.setItem(slot, createNodeItem(player, session, node));
      }
    }
  }

  private void renderConnections(Gui gui, Player player, EditorSession session) {
    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();

    boolean isPathEditMode = session.isPathEditMode();

    // Render tree-level path points
    for (Position p : tree.paths()) {
      int canvasX = p.x() - scrollX;
      int canvasY = p.y() - scrollY;

      if (canvasX >= 0 && canvasX < GUI_COLS && canvasY >= 0 && canvasY < CANVAS_ROWS) {
        int slot = (canvasY + CANVAS_START_ROW) * GUI_COLS + canvasX;

        // Use different colors for path edit mode
        Material material = isPathEditMode
            ? Material.LIME_STAINED_GLASS_PANE
            : Material.WHITE_STAINED_GLASS_PANE;
        NamedTextColor color = isPathEditMode
            ? NamedTextColor.GREEN
            : NamedTextColor.WHITE;
        String title = "Path Point";

        List<String> lore = new ArrayList<>();
        lore.add(LEGACY.serialize(Component.text("Position: (" + p.x() + ", " + p.y() + ")", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));

        if (isPathEditMode) {
          lore.add(LEGACY.serialize(Component.empty()));
          lore.add(LEGACY.serialize(Component.text("Click to remove path point", NamedTextColor.RED)
              .decoration(TextDecoration.ITALIC, true)));
        }

        // Capture position for lambda
        Position pathPosition = p;

        GuiItem connection = ItemBuilder.from(material)
            .setName(LEGACY.serialize(Component.text(title, color)
                .decoration(TextDecoration.ITALIC, false)))
            .setLore(lore)
            .asGuiItem(event -> {
              event.setCancelled(true);
              if (session.isPathEditMode()) {
                // Remove path point from tree
                session.saveSnapshot();
                tree.paths().remove(pathPosition);
                Mint.sendThemedMessage(player, "<accent>Removed path point at (" + pathPosition.x() + ", " + pathPosition.y() + ")");
                refresh(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
              }
            });
        gui.setItem(slot, connection);
      }
    }
  }

  private GuiItem createNodeItem(Player player, EditorSession session, EditorNode node) {
    boolean isSelected = node.id().equals(session.selectedNodeId());

    NamedTextColor nameColor = isSelected ? NamedTextColor.AQUA : NamedTextColor.YELLOW;
    String prefix = isSelected ? "[SELECTED] " : "";

    ItemBuilder builder = ItemBuilder.from(node.icon())
        .setName(LEGACY.serialize(Component.text(prefix + node.name(), nameColor)
            .decoration(TextDecoration.ITALIC, false)));

    // Build lore
    List<String> lore = new ArrayList<>();
    lore.add(LEGACY.serialize(Component.text("ID: " + node.id(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false)));

    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(LEGACY.serialize(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
      }
    }

    lore.add(LEGACY.serialize(Component.empty()));
    lore.add(LEGACY.serialize(Component.text("Cost: " + node.cost() + " SP", NamedTextColor.AQUA)
        .decoration(TextDecoration.ITALIC, false)));

    if (node.archetypeRef() != null) {
      lore.add(LEGACY.serialize(Component.text("Archetype: " + node.archetypeRef(), NamedTextColor.LIGHT_PURPLE)
          .decoration(TextDecoration.ITALIC, false)));
    }

    // Prerequisites
    if (!node.prerequisites().isEmpty()) {
      lore.add(LEGACY.serialize(Component.empty()));
      lore.add(LEGACY.serialize(Component.text("Prerequisites:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false)));
      for (String prereq : node.prerequisites()) {
        lore.add(LEGACY.serialize(Component.text("  - " + prereq, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
      }
    }

    // Children
    if (!node.children().isEmpty()) {
      lore.add(LEGACY.serialize(Component.empty()));
      lore.add(LEGACY.serialize(Component.text("Children:", NamedTextColor.GREEN)
          .decoration(TextDecoration.ITALIC, false)));
      for (String child : node.children()) {
        lore.add(LEGACY.serialize(Component.text("  - " + child, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
      }
    }

    // Effects
    if (!node.effects().isEmpty()) {
      lore.add(LEGACY.serialize(Component.empty()));
      lore.add(LEGACY.serialize(Component.text("Effects:", NamedTextColor.LIGHT_PURPLE)
          .decoration(TextDecoration.ITALIC, false)));
      for (EditorEffect effect : node.effects()) {
        lore.add(LEGACY.serialize(Component.text("  - " + effect.getDisplayDescription(), NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)));
      }
    }

    lore.add(LEGACY.serialize(Component.empty()));
    lore.add(LEGACY.serialize(Component.text("Right-click to edit", NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, true)));
    lore.add(LEGACY.serialize(Component.text("Shift+click to link/unlink", NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, true)));
    lore.add(LEGACY.serialize(Component.text("Press Q to delete", NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, true)));

    builder.setLore(lore);

    // Add enchantment for selected nodes (visual indicator)
    if (isSelected) {
      builder.enchant(Enchantment.UNBREAKING);
    }

    return builder.asGuiItem(event -> handleNodeClick(player, session, node, event));
  }

  private ItemStack createControlItem(Material material, String action, String name,
                                      NamedTextColor color, String... loreLines) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(Component.text(name, color)
        .decoration(TextDecoration.ITALIC, false));

    if (loreLines.length > 0) {
      List<Component> lore = new ArrayList<>();
      for (String line : loreLines) {
        lore.add(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
      }
      meta.lore(lore);
    }

    // Store action in display name parsing (we'll parse this in click handler)
    // Use a simple approach - the name determines the action
    item.setItemMeta(meta);
    return item;
  }

  private void setupHotbarControls(Player player, EditorSession session) {
    EditorTree tree = session.tree();

    // Slot 0: Scroll Up
    player.getInventory().setItem(0, createControlItem(
        session.scrollOffsetY() > 0 ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
        "scroll_up",
        session.scrollOffsetY() > 0 ? "Scroll Up" : "At Top",
        session.scrollOffsetY() > 0 ? NamedTextColor.AQUA : NamedTextColor.GRAY
    ));

    // Slot 1: Scroll Down
    player.getInventory().setItem(1, createControlItem(
        Material.ARROW,
        "scroll_down",
        "Scroll Down",
        NamedTextColor.AQUA
    ));

    // Slot 2: Add Node
    player.getInventory().setItem(2, createControlItem(
        session.isDragging() ? Material.LIME_CONCRETE : Material.LIME_STAINED_GLASS_PANE,
        "add_node",
        session.isDragging() ? "PLACING... Click slot" : "Add Node",
        session.isDragging() ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN,
        "Click, then click empty slot"
    ));

    // Slot 3: Undo
    player.getInventory().setItem(3, createControlItem(
        session.canUndo() ? Material.ORANGE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
        "undo",
        "Undo",
        session.canUndo() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY
    ));

    // Slot 4: Tree Info
    ItemStack info = new ItemStack(Material.OAK_SIGN);
    ItemMeta infoMeta = info.getItemMeta();
    infoMeta.displayName(Component.text(tree.displayName(), NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false));
    List<Component> infoLore = new ArrayList<>();
    infoLore.add(Component.text("Job: " + tree.jobKey(), NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false));
    infoLore.add(Component.text("Nodes: " + tree.nodes().size(), NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));
    infoLore.add(Component.text("Row: " + session.scrollOffsetY(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false));
    if (session.selectedNodeId() != null) {
      infoLore.add(Component.empty());
      infoLore.add(Component.text("Selected: " + session.selectedNodeId(), NamedTextColor.AQUA)
          .decoration(TextDecoration.ITALIC, false));
    }
    infoMeta.lore(infoLore);
    info.setItemMeta(infoMeta);
    player.getInventory().setItem(4, info);

    // Slot 5: Redo
    player.getInventory().setItem(5, createControlItem(
        session.canRedo() ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
        "redo",
        "Redo",
        session.canRedo() ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY
    ));

    // Slot 6: Save
    player.getInventory().setItem(6, createControlItem(
        Material.WRITABLE_BOOK,
        "save",
        "Save Tree",
        NamedTextColor.GREEN,
        "Save to file",
        "Stored in upgrade_trees folder"
    ));

    // Slot 7: Path Edit Mode
    player.getInventory().setItem(7, createControlItem(
        session.isPathEditMode() ? Material.LEAD : Material.STRING,
        "path_edit",
        session.isPathEditMode() ? "PATH MODE (Active)" : "Edit Paths",
        session.isPathEditMode() ? NamedTextColor.GREEN : NamedTextColor.GRAY,
        "Toggle path editing mode",
        "Select node, then click slots",
        "to add/remove path points"
    ));

    // Slot 8: Settings
    player.getInventory().setItem(8, createControlItem(
        Material.REDSTONE_TORCH,
        "settings",
        "Tree Settings",
        NamedTextColor.RED,
        "Edit tree properties",
        "Archetypes, policies, etc."
    ));
  }

  // ========== Event Handling ==========

  private void handleHotbarClick(Player player, EditorSession session, InventoryClickEvent event) {
    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) return;

    ItemMeta meta = clicked.getItemMeta();
    if (meta == null) return;

    String displayName = meta.hasDisplayName() ?
        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName()) : "";

    String action = getActionFromItem(displayName);
    if (action != null) {
      handleControlAction(player, session, action);
    }
    event.setCancelled(true);
  }

  private String getActionFromItem(String displayName) {
    return switch (displayName) {
      case "Scroll Up" -> "scroll_up";
      case "At Top" -> "scroll_up";
      case "Scroll Down" -> "scroll_down";
      case "Add Node", "PLACING... Click slot" -> "add_node";
      case "Undo" -> "undo";
      case "Redo" -> "redo";
      case "Save Tree" -> "save";
      case "Tree Settings" -> "settings";
      case "Edit Paths", "PATH MODE (Active)" -> "path_edit";
      default -> null;
    };
  }

  private void handleNodeClick(Player player, EditorSession session, EditorNode node, InventoryClickEvent event) {
    ClickType click = event.getClick();
    EditorTree tree = session.tree();

    // Right-click opens node editor
    if (click.isRightClick()) {
      event.setCancelled(true);
      transitioningToSubGui.add(player.getUniqueId());
      nodeEditorGui.open(player, session, node);
      return;
    }

    if (click == org.bukkit.event.inventory.ClickType.DROP) {
      // Delete node
      event.setCancelled(true);
      if (node.id().equals(tree.rootNodeId())) {
        Mint.sendThemedMessage(player, "<error>Cannot delete root node!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        return;
      }

      session.saveSnapshot();
      tree.removeNode(node.id());
      if (node.id().equals(session.selectedNodeId())) {
        session.selectNode(null);
      }
      Mint.sendThemedMessage(player, "<accent>Deleted node: <secondary>" + node.id());
      refresh(player);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
      return;
    }

    if (click.isShiftClick()) {
      // Link/unlink with selected node
      event.setCancelled(true);
      String selectedId = session.selectedNodeId();
      if (selectedId == null || selectedId.equals(node.id())) {
        Mint.sendThemedMessage(player, "<error>Select a different node first!");
        return;
      }

      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isEmpty()) return;

      EditorNode selected = selectedOpt.get();

      session.saveSnapshot();

      // Toggle connection: if selected is parent of clicked, remove; otherwise add
      if (selected.children().contains(node.id())) {
        // Remove connection
        selected.children().remove(node.id());
        node.prerequisites().remove(selectedId);
        Mint.sendThemedMessage(player, "<accent>Removed link: <secondary>" + selectedId + " -> " + node.id());
      } else {
        // Add connection
        selected.children().add(node.id());
        node.prerequisites().add(selectedId);
        Mint.sendThemedMessage(player, "<success>Added link: <secondary>" + selectedId + " -> " + node.id());
      }

      refresh(player);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
      return;
    }

    // Left click: select/deselect
    event.setCancelled(true);
    if (node.id().equals(session.selectedNodeId())) {
      session.selectNode(null);
      Mint.sendThemedMessage(player, "<accent>Deselected node");
    } else {
      session.selectNode(node.id());
      Mint.sendThemedMessage(player, "<accent>Selected: <secondary>" + node.id());
    }
    refresh(player);
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
  }

  private void handleControlAction(Player player, EditorSession session, String action) {
    switch (action) {
      case "scroll_up" -> {
        if (session.scrollOffsetY() > 0) {
          session.setScrollOffsetY(Math.max(0, session.scrollOffsetY() - CANVAS_ROWS));
          refresh(player);
          player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
      }
      case "scroll_down" -> {
        session.setScrollOffsetY(session.scrollOffsetY() + CANVAS_ROWS);
        refresh(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }
      case "add_node" -> {
        Mint.sendThemedMessage(player, "<accent>Click an empty slot to place a new node");
        session.setDragging(true);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
      }
      case "undo" -> {
        if (session.undo()) {
          Mint.sendThemedMessage(player, "<accent>Undone");
          refresh(player);
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
        }
      }
      case "redo" -> {
        if (session.redo()) {
          Mint.sendThemedMessage(player, "<accent>Redone");
          refresh(player);
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.2f);
        }
      }
      case "save" -> {
        EditorTree tree = session.tree();
        String json = exporter.exportSingle(tree);
        String treeId = tree.treeId();

        if (treeLoader.saveTree(treeId, json)) {
          Mint.sendThemedMessage(player, "<success>Saved tree '<secondary>" + treeId +
              "<success>' to <primary>upgrade_trees/" + treeId + ".json");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.0f);
        } else {
          Mint.sendThemedMessage(player, "<error>Failed to save tree. Check server logs.");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
      }
      case "settings" -> {
        transitioningToSubGui.add(player.getUniqueId());
        settingsGui.open(player, session);
      }
      case "path_edit" -> {
        session.setPathEditMode(!session.isPathEditMode());
        if (session.isPathEditMode()) {
          Mint.sendThemedMessage(player, "<accent>Path edit mode <success>enabled<accent>. Click empty slots to add path points, click existing paths to remove.");
        } else {
          Mint.sendThemedMessage(player, "<accent>Path edit mode <error>disabled<accent>.");
        }
        refresh(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, session.isPathEditMode() ? 1.5f : 0.8f);
      }
    }
  }

  private void handleEmptySlotClick(Player player, EditorSession session, int slot) {
    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();

    // Calculate world position from slot
    int canvasX = slot % GUI_COLS;
    int canvasY = slot / GUI_COLS;
    int worldX = canvasX + scrollX;
    int worldY = canvasY + scrollY;
    Position newPos = new Position(worldX, worldY);

    // Check if we're in "add node" mode
    if (session.isDragging()) {
      session.setDragging(false);
      session.saveSnapshot();

      // Create new node at this position
      String nodeId = "node_" + System.currentTimeMillis();
      EditorNode newNode = new EditorNode();
      newNode.setId(nodeId);
      newNode.setPosition(newPos);
      tree.addNode(newNode);

      Mint.sendThemedMessage(player, "<success>Created node: <secondary>" + nodeId + " <success>at (" + worldX + ", " + worldY + ")");
      refresh(player);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
      return;
    }

    // Check if we're in path edit mode
    if (session.isPathEditMode()) {
      // Check if position already has a path point
      boolean pathExists = tree.paths().stream()
          .anyMatch(p -> p.x() == worldX && p.y() == worldY);

      if (pathExists) {
        Mint.sendThemedMessage(player, "<error>Path point already exists at this position!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        return;
      }

      // Add path point to tree
      session.saveSnapshot();
      tree.paths().add(newPos);
      Mint.sendThemedMessage(player, "<accent>Added path point at (" + worldX + ", " + worldY + ")");
      refresh(player);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
      return;
    }

    // Check if a node is selected - move it to this position
    String selectedId = session.selectedNodeId();
    if (selectedId != null) {
      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isPresent()) {
        EditorNode selected = selectedOpt.get();
        session.saveSnapshot();
        selected.setPosition(newPos);
        Mint.sendThemedMessage(player, "<accent>Moved <secondary>" + selectedId + " <accent>to (" + worldX + ", " + worldY + ")");
        refresh(player);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.2f);
      }
    }
  }

  private void handleClose(Player player) {
    UUID playerId = player.getUniqueId();

    // If transitioning to a sub-GUI (node editor, settings), don't clean up session
    if (transitioningToSubGui.remove(playerId)) {
      openGuis.remove(playerId);
      return;
    }

    EditorSession session = sessions.remove(playerId);
    openGuis.remove(playerId);

    if (session == null) return;

    // Restore player inventory
    ItemStack[] saved = savedInventories.remove(playerId);
    if (saved != null) {
      player.getInventory().clear();
      player.getInventory().setContents(saved);
      player.updateInventory();
    }

    Mint.sendThemedMessage(player, "<accent>Closed tree editor. Use <secondary>/jobs treeeditor<accent> to reopen.");
  }

  /**
   * Reopen the main editor for a player (called from sub-GUIs).
   */
  public void reopenFor(Player player) {
    UUID playerId = player.getUniqueId();

    EditorSession session = sessions.get(playerId);
    if (session == null) return;

    Gui gui = createGui(player, session);
    gui.open(player);
    openGuis.put(playerId, gui);
  }
}
