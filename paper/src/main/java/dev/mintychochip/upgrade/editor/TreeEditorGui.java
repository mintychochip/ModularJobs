package dev.mintychochip.upgrade.editor;

import dev.craftux.api.inventory.ClickKind;
import dev.craftux.api.inventory.InteractionPolicy;
import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.ItemSpec;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import dev.mintychochip.gui.craftux.CraftuxItems;
import dev.mintychochip.gui.craftux.CraftuxUiHost;
import dev.mintychochip.upgrade.Position;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.config.UpgradeTreeLoader;
import dev.mintychochip.util.Messages;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Upgrade tree visual editor using craftux inventory sessions.
 *
 * <p>Toolbar controls live in the bottom inventory row (craftux top-window only). Sub-editors
 * (node/settings) are separate craftux views opened by host actions.
 */
public final class TreeEditorGui {

  private static final int GUI_ROWS = 6;
  private static final int GUI_COLS = 9;
  private static final int GUI_SIZE = 54;
  private static final int CANVAS_SLOTS = 45; // rows 0-4; row 5 = toolbar
  private static final int TOOLBAR_START = 45;
  private static final String MENU_ID = "tree_editor";

  private final InventoryRuntime inventory;
  private final TreeEditorExporter exporter;
  private final UpgradeTreeLoader treeLoader;
  private final TreeEditorNodeGui nodeEditorGui;
  private final TreeEditorSettingsGui settingsGui;

  private final Map<UUID, EditorSession> sessions = new HashMap<>();
  private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
  private final Set<UUID> transitioningToSubGui = new HashSet<>();
  private final Map<UUID, Map<Integer, String>> slotNodes = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotControls = new HashMap<>();

  /** Tree editor gui. */
  public TreeEditorGui(
      Plugin plugin,
      InventoryRuntime inventory,
      TreeEditorExporter exporter,
      UpgradeTreeLoader treeLoader,
      TreeEditorNodeGui nodeEditorGui,
      TreeEditorSettingsGui settingsGui) {
    // plugin reserved for future editor messaging/scheduling hooks
    if (plugin == null) {
      throw new IllegalArgumentException("plugin must not be null");
    }
    this.inventory = inventory;
    this.exporter = exporter;
    this.treeLoader = treeLoader;
    this.nodeEditorGui = nodeEditorGui;
    this.settingsGui = settingsGui;
    nodeEditorGui.setMainEditor(this);
    settingsGui.setMainEditor(this);
  }

  /** Open. */
  public void open(@NotNull Player player, @NotNull UpgradeTree tree) {
    openEditor(player, EditorTree.fromUpgradeTree(tree));
  }

  /** Open new. */
  public void openNew(@NotNull Player player, @NotNull String jobKey) {
    openEditor(player, EditorTree.createBlank(jobKey));
  }

  private void openEditor(@NotNull Player player, @NotNull EditorTree tree) {
    UUID playerId = player.getUniqueId();
    EditorSession session = new EditorSession(playerId, tree);
    sessions.put(playerId, session);
    savedInventories.put(playerId, player.getInventory().getContents().clone());
    player.getInventory().clear();
    inventory.open(playerId, buildView(player, session));
  }

  /** Refresh. */
  public void refresh(@NotNull Player player) {
    UUID playerId = player.getUniqueId();
    EditorSession session = sessions.get(playerId);
    if (session == null) {
      return;
    }
    inventory.refresh(playerId, buildView(player, session));
  }

  /** Returns the session. */
  public Optional<EditorSession> getSession(@NotNull Player player) {
    return Optional.ofNullable(sessions.get(player.getUniqueId()));
  }

  /** Reopen for. */
  public void reopenFor(Player player) {
    UUID playerId = player.getUniqueId();
    EditorSession session = sessions.get(playerId);
    if (session == null) {
      return;
    }
    inventory.open(playerId, buildView(player, session));
  }

  /** On canvas click. */
  public void onCanvasClick(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    EditorSession session = sessions.get(audience);
    if (player == null || session == null) {
      return;
    }
    handleEmptySlotClick(player, session, click.slot());
  }

  /** On node click. */
  public void onNodeClick(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    EditorSession session = sessions.get(audience);
    if (player == null || session == null) {
      return;
    }
    Map<Integer, String> nodes = slotNodes.get(audience);
    if (nodes == null) {
      return;
    }
    String nodeId = nodes.get(click.slot());
    if (nodeId == null) {
      return;
    }
    Optional<EditorNode> nodeOpt = session.tree().getNode(nodeId);
    if (nodeOpt.isEmpty()) {
      return;
    }
    handleNodeClick(player, session, nodeOpt.get(), click);
  }

  /** On control click. */
  public void onControlClick(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    EditorSession session = sessions.get(audience);
    if (player == null || session == null) {
      return;
    }
    Map<Integer, String> controls = slotControls.get(audience);
    if (controls == null) {
      return;
    }
    String action = controls.get(click.slot());
    if (action != null) {
      handleControlAction(player, session, action);
    }
  }

  InventoryView buildView(Player player, EditorSession session) {
    final UUID audience = player.getUniqueId();
    Map<Integer, Slot> slots = new HashMap<>();
    Map<Integer, String> nodes = new HashMap<>();
    final Map<Integer, String> controls = new HashMap<>();

    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();

    ItemSpec empty = CraftuxItems.pane(Material.BLACK_STAINED_GLASS_PANE);
    for (int i = 0; i < CANVAS_SLOTS; i++) {
      slots.put(
          i,
          Slot.button(
              "canvas_" + i,
              empty,
              CraftuxUiHost.ACTION_EDITOR_CANVAS,
              SlotPixelIntent.UNVALIDATED));
    }

    // Path points
    for (Position path : tree.paths()) {
      int sx = path.x() - scrollX;
      int sy = path.y() - scrollY;
      if (sx < 0 || sx >= GUI_COLS || sy < 0 || sy >= 5) {
        continue;
      }
      int slot = sy * GUI_COLS + sx;
      slots.put(
          slot,
          Slot.button(
              "canvas_" + slot,
              CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE),
              CraftuxUiHost.ACTION_EDITOR_CANVAS,
              SlotPixelIntent.UNVALIDATED));
    }

    // Nodes
    for (EditorNode node : tree.nodes().values()) {
      Position pos = node.position();
      if (pos == null) {
        continue;
      }
      int sx = pos.x() - scrollX;
      int sy = pos.y() - scrollY;
      if (sx < 0 || sx >= GUI_COLS || sy < 0 || sy >= 5) {
        continue;
      }
      int slot = sy * GUI_COLS + sx;
      boolean selected = node.id().equals(session.selectedNodeId());
      Material mat = node.icon() != null ? node.icon() : Material.PAPER;
      String label = (selected ? "★ " : "") + node.name();
      List<String> lore = new ArrayList<>();
      lore.add("ID: " + node.id());
      lore.add("Left: select | Right: edit | Shift: link | Q: delete");
      slots.put(
          slot,
          Slot.button(
              "node." + sanitize(node.id()),
              CraftuxItems.of(mat, label, lore),
              CraftuxUiHost.ACTION_EDITOR_NODE,
              SlotPixelIntent.UNVALIDATED));
      nodes.put(slot, node.id());
    }

    // Toolbar row
    placeToolbar(slots, controls, session);

    slotNodes.put(audience, Map.copyOf(nodes));
    slotControls.put(audience, Map.copyOf(controls));

    String title = "Tree Editor: " + session.tree().displayName();
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }
    InventoryView.Builder builder =
        InventoryView.builder(MENU_ID, GUI_ROWS)
            .title(title)
            .interactionPolicy(
                new InteractionPolicy(
                    EnumSet.of(
                        ClickKind.LEFT,
                        ClickKind.RIGHT,
                        ClickKind.SHIFT_LEFT,
                        ClickKind.SHIFT_RIGHT,
                        ClickKind.DROP),
                    true,
                    true));
    for (int i = 0; i < GUI_SIZE; i++) {
      Slot s = slots.get(i);
      if (s != null) {
        builder.slot(i, s);
      } else {
        builder.decorative(i, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE));
      }
    }
    return builder.build();
  }

  private void placeToolbar(
      Map<Integer, Slot> slots, Map<Integer, String> controls, EditorSession session) {
    putControl(slots, controls, TOOLBAR_START, Material.ARROW, "scroll_up", "Scroll Up");
    putControl(slots, controls, TOOLBAR_START + 1, Material.ARROW, "scroll_down", "Scroll Down");
    putControl(slots, controls, TOOLBAR_START + 2, Material.EMERALD, "add_node", "Add Node");
    putControl(slots, controls, TOOLBAR_START + 3, Material.IRON_AXE, "undo", "Undo");
    putControl(slots, controls, TOOLBAR_START + 4, Material.GOLDEN_AXE, "redo", "Redo");
    putControl(slots, controls, TOOLBAR_START + 5, Material.WRITABLE_BOOK, "save", "Save Tree");
    Material pathMat = session.isPathEditMode() ? Material.LEAD : Material.STRING;
    String pathLabel = session.isPathEditMode() ? "PATH MODE (Active)" : "Edit Paths";
    putControl(slots, controls, TOOLBAR_START + 6, pathMat, "path_edit", pathLabel);
    putControl(
        slots, controls, TOOLBAR_START + 7, Material.REDSTONE_TORCH, "settings", "Tree Settings");
    slots.put(
        TOOLBAR_START + 8, Slot.decorative(CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE)));
  }

  private void putControl(
      Map<Integer, Slot> slots,
      Map<Integer, String> controls,
      int index,
      Material material,
      String action,
      String label) {
    slots.put(
        index,
        Slot.navigation(
            "ctrl." + action,
            CraftuxItems.of(material, label),
            CraftuxUiHost.ACTION_EDITOR_CONTROL,
            SlotPixelIntent.UNVALIDATED));
    controls.put(index, action);
  }

  private void handleNodeClick(
      Player player, EditorSession session, EditorNode node, InventoryClick click) {
    EditorTree tree = session.tree();
    var kind = click.policyKind();

    if (kind == ClickKind.RIGHT) {
      transitioningToSubGui.add(player.getUniqueId());
      nodeEditorGui.open(player, session, node);
      return;
    }

    if (kind == ClickKind.DROP) {
      if (node.id().equals(tree.rootNodeId())) {
        Messages.send(player, "<error>Cannot delete root node!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        return;
      }
      session.saveSnapshot();
      tree.removeNode(node.id());
      if (node.id().equals(session.selectedNodeId())) {
        session.selectNode(null);
      }
      Messages.send(player, "<accent>Deleted node: <secondary>" + node.id());
      refresh(player);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
      return;
    }

    if (kind == ClickKind.SHIFT_LEFT || kind == ClickKind.SHIFT_RIGHT) {
      String selectedId = session.selectedNodeId();
      if (selectedId == null || selectedId.equals(node.id())) {
        Messages.send(player, "<error>Select a different node first!");
        return;
      }
      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isEmpty()) {
        return;
      }
      EditorNode selected = selectedOpt.get();
      session.saveSnapshot();
      if (selected.children().contains(node.id())) {
        selected.children().remove(node.id());
        node.prerequisites().remove(selectedId);
        Messages.send(
            player, "<accent>Removed link: <secondary>" + selectedId + " -> " + node.id());
      } else {
        selected.children().add(node.id());
        node.prerequisites().add(selectedId);
        Messages.send(player, "<success>Added link: <secondary>" + selectedId + " -> " + node.id());
      }
      refresh(player);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
      return;
    }

    if (node.id().equals(session.selectedNodeId())) {
      session.selectNode(null);
      Messages.send(player, "<accent>Deselected node");
    } else {
      session.selectNode(node.id());
      Messages.send(player, "<accent>Selected: <secondary>" + node.id());
    }
    refresh(player);
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
  }

  private void handleControlAction(Player player, EditorSession session, String action) {
    switch (action) {
      case "scroll_up" -> {
        if (session.scrollOffsetY() > 0) {
          session.setScrollOffsetY(Math.max(0, session.scrollOffsetY() - 5));
          refresh(player);
        }
      }
      case "scroll_down" -> {
        session.setScrollOffsetY(session.scrollOffsetY() + 5);
        refresh(player);
      }
      case "add_node" -> {
        Messages.send(player, "<accent>Click an empty slot to place a new node");
        session.setDragging(true);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
      }
      case "undo" -> {
        if (session.undo()) {
          Messages.send(player, "<accent>Undone");
          refresh(player);
        }
      }
      case "redo" -> {
        if (session.redo()) {
          Messages.send(player, "<accent>Redone");
          refresh(player);
        }
      }
      case "save" -> {
        EditorTree tree = session.tree();
        String json = exporter.exportSingle(tree);
        String treeId = tree.treeId();
        if (treeLoader.saveTree(treeId, json)) {
          Messages.send(
              player,
              "<success>Saved tree '<secondary>"
                  + treeId
                  + "<success>' to <primary>upgrade_trees/"
                  + treeId
                  + ".json");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.0f);
        } else {
          Messages.send(player, "<error>Failed to save tree. Check server logs.");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
      }
      case "settings" -> {
        transitioningToSubGui.add(player.getUniqueId());
        settingsGui.open(player, session);
      }
      case "path_edit" -> {
        session.setPathEditMode(!session.isPathEditMode());
        Messages.send(
            player,
            session.isPathEditMode()
                ? "<accent>Path edit mode <success>enabled"
                : "<accent>Path edit mode <error>disabled");
        refresh(player);
      }
      default -> {}
    }
  }

  private void handleEmptySlotClick(Player player, EditorSession session, int slot) {
    if (slot >= CANVAS_SLOTS) {
      return;
    }
    EditorTree tree = session.tree();
    int scrollX = session.scrollOffsetX();
    int scrollY = session.scrollOffsetY();
    int canvasX = slot % GUI_COLS;
    int canvasY = slot / GUI_COLS;
    int worldX = canvasX + scrollX;
    int worldY = canvasY + scrollY;
    Position newPos = new Position(worldX, worldY);

    if (session.isDragging()) {
      session.setDragging(false);
      session.saveSnapshot();
      String nodeId = "node_" + System.currentTimeMillis();
      EditorNode newNode = new EditorNode();
      newNode.setId(nodeId);
      newNode.setPosition(newPos);
      tree.addNode(newNode);
      Messages.send(player, "<success>Created node: <secondary>" + nodeId);
      refresh(player);
      return;
    }

    if (session.isPathEditMode()) {
      boolean pathExists = tree.paths().stream().anyMatch(p -> p.x() == worldX && p.y() == worldY);
      if (pathExists) {
        session.saveSnapshot();
        tree.paths().removeIf(p -> p.x() == worldX && p.y() == worldY);
        Messages.send(player, "<accent>Removed path point at (" + worldX + ", " + worldY + ")");
      } else {
        session.saveSnapshot();
        tree.paths().add(newPos);
        Messages.send(player, "<accent>Added path point at (" + worldX + ", " + worldY + ")");
      }
      refresh(player);
      return;
    }

    String selectedId = session.selectedNodeId();
    if (selectedId != null) {
      Optional<EditorNode> selectedOpt = tree.getNode(selectedId);
      if (selectedOpt.isPresent()) {
        session.saveSnapshot();
        selectedOpt.get().setPosition(newPos);
        Messages.send(
            player,
            "<accent>Moved <secondary>"
                + selectedId
                + " <accent>to ("
                + worldX
                + ", "
                + worldY
                + ")");
        refresh(player);
      }
    }
  }

  /** Called when craftux closes the inventory (player Esc / host close). */
  public void onSessionClosed(UUID audience) {
    if (transitioningToSubGui.remove(audience)) {
      return;
    }
    EditorSession session = sessions.remove(audience);
    slotNodes.remove(audience);
    slotControls.remove(audience);
    if (session == null) {
      return;
    }
    Player player = Bukkit.getPlayer(audience);
    ItemStack[] saved = savedInventories.remove(audience);
    if (player != null && saved != null) {
      player.getInventory().clear();
      player.getInventory().setContents(saved);
      player.updateInventory();
      Messages.send(
          player, "<accent>Closed tree editor. Use <secondary>/jobs treeeditor<accent> to reopen.");
    }
  }

  private static String sanitize(String id) {
    return id.replace(':', '.').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
  }
}
