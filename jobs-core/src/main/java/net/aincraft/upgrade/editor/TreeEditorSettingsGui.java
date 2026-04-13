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
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Sub-GUI for editing tree-level settings using Triumph GUI.
 */
public final class TreeEditorSettingsGui implements Listener {

  private static final int GUI_SIZE = 54;
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  private final Plugin plugin;

  // Injected via setter to avoid circular dependency
  private TreeEditorGui mainEditor;

  // Active settings edit sessions
  private final Map<UUID, SettingsEditSession> editSessions = new HashMap<>();

  // Chat input listeners
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();

  // Store open GUIs
  private final Map<UUID, Gui> openGuis = new HashMap<>();

  private record SettingsEditSession(EditorSession editorSession) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(String input);
  }

  @Inject
  public TreeEditorSettingsGui(Plugin plugin) {
    this.plugin = plugin;
    // Register as listener for cleanup on player quit
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  /**
   * Setter injection for TreeEditorGui to break circular dependency.
   */
  @Inject
  public void setMainEditor(TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  /**
   * Open the settings editor for the current session.
   */
  public void open(@NotNull Player player, @NotNull EditorSession session) {
    UUID playerId = player.getUniqueId();

    // Store edit session
    editSessions.put(playerId, new SettingsEditSession(session));

    // Create and open GUI
    Gui gui = createGui(player, session);
    gui.open(player);
    openGuis.put(playerId, gui);
  }

  private Gui createGui(Player player, EditorSession session) {
    EditorTree tree = session.tree();

    Component title = Component.text()
        .append(Component.text("Tree Settings: ", NamedTextColor.DARK_GRAY))
        .append(Component.text(tree.displayName(), NamedTextColor.GOLD))
        .build();

    Gui gui = Gui.gui()
        .title(title)
        .rows(6)
        .create();

    // Cancel all clicks by default
    gui.setDefaultClickAction(event -> event.setCancelled(true));

    // Set close handler
    gui.setCloseGuiAction(event -> {
      Player p = (Player) event.getPlayer();
      handleClose(p);
    });

    // Register chat listener
    if (!chatInputHandlers.containsKey(player.getUniqueId())) {
      plugin.getServer().getPluginManager().registerEvents(
          new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
              handleChatEvent(event);
            }
          },
          plugin
      );
    }

    renderGui(gui, session, player);

    return gui;
  }

  private void renderGui(Gui gui, EditorSession session, Player player) {
    EditorTree tree = session.tree();

    // Fill background
    GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
        .setName(" ")
        .asGuiItem();
    for (int i = 0; i < GUI_SIZE; i++) {
      gui.setItem(i, pane);
    }

    // Row 0: Back button and tree info
    gui.setItem(0, createActionItem(Material.ARROW, "back", "Back", NamedTextColor.WHITE,
        "Return to tree editor"));

    // Tree preview
    ItemStack preview = new ItemStack(Material.OAK_SIGN);
    ItemMeta previewMeta = preview.getItemMeta();
    previewMeta.displayName(Component.text(tree.treeId(), NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false));
    List<Component> previewLore = new ArrayList<>();
    previewLore.add(Component.text("Job: " + tree.jobKey(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false));
    previewLore.add(Component.text("Nodes: " + tree.nodes().size(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false));
    previewMeta.lore(previewLore);
    preview.setItemMeta(previewMeta);
    gui.setItem(4, ItemBuilder.from(preview).asGuiItem(event -> event.setCancelled(true)));

    // Row 1: Basic properties
    gui.setItem(10, createPropertyItem(Material.NAME_TAG, "display_name",
        "Display Name", tree.displayName(), "Click to edit"));

    gui.setItem(11, createPropertyItem(Material.EXPERIENCE_BOTTLE, "skill_points",
        "Skill Points per Level", String.valueOf(tree.skillPointsPerLevel()),
        "Left-click: +1", "Right-click: -1", "Shift: +/-5"));

    gui.setItem(12, createPropertyItem(Material.REDSTONE_TORCH, "root_node",
        "Root Node ID", tree.rootNodeId(), "Click to view/edit"));

    // Row 2: Archetypes section
    gui.setItem(19, createActionItem(Material.BLAZE_POWDER, "add_archetype",
        "Add Archetype", NamedTextColor.GREEN, "Add a new archetype"));

    // Archetype header
    gui.setItem(20, ItemBuilder.from(Material.WRITABLE_BOOK)
        .setName(LEGACY.serialize(Component.text("Archetypes", NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false)))
        .asGuiItem(event -> event.setCancelled(true)));

    // Show existing archetypes (up to 5)
    List<EditorTree.EditorArchetype> archetypes = tree.archetypes();
    for (int i = 0; i < Math.min(archetypes.size(), 5); i++) {
      EditorTree.EditorArchetype archetype = archetypes.get(i);
      gui.setItem(21 + i, createArchetypeItem(session, archetype, i, player));
    }

    // Row 4: Perk policies section
    gui.setItem(37, createActionItem(Material.ENCHANTED_BOOK, "add_policy",
        "Add Perk Policy", NamedTextColor.GREEN, "Add a new perk policy"));

    // Perk policies header
    gui.setItem(38, ItemBuilder.from(Material.WRITTEN_BOOK)
        .setName(LEGACY.serialize(Component.text("Perk Policies", NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)))
        .asGuiItem(event -> event.setCancelled(true)));

    // Show existing perk policies (up to 6)
    int policyIndex = 0;
    for (Map.Entry<String, String> entry : tree.perkPolicies().entrySet()) {
      if (policyIndex >= 6) break;
      gui.setItem(39 + policyIndex, createPolicyItem(session, entry.getKey(), entry.getValue(), policyIndex, player));
      policyIndex++;
    }
  }

  private GuiItem createActionItem(Material material, String action, String name,
      NamedTextColor color, String... loreLines) {
    ItemBuilder builder = ItemBuilder.from(material)
        .setName(LEGACY.serialize(Component.text(name, color)
            .decoration(TextDecoration.ITALIC, false)));

    if (loreLines.length > 0) {
      List<String> lore = new ArrayList<>();
      for (String line : loreLines) {
        lore.add(LEGACY.serialize(Component.text(line, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
      }
      builder.setLore(lore);
    }

    return builder.asGuiItem(event -> handleActionClick(event, action));
  }

  private GuiItem createPropertyItem(Material material, String action, String label,
      String value, String... hints) {
    ItemBuilder builder = ItemBuilder.from(material)
        .setName(LEGACY.serialize(Component.text(label, NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false)));

    List<String> lore = new ArrayList<>();
    lore.add(LEGACY.serialize(Component.text("Current: " + value, NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.empty()));
    for (String hint : hints) {
      lore.add(LEGACY.serialize(Component.text(hint, NamedTextColor.GRAY)
          .decoration(TextDecoration.ITALIC, false)));
    }
    builder.setLore(lore);

    return builder.asGuiItem(event -> handlePropertyClick(event, action));
  }

  private GuiItem createArchetypeItem(EditorSession session, EditorTree.EditorArchetype archetype, int index, Player player) {
    // Map color names to materials
    Material colorMaterial = switch (archetype.color()) {
      case "red" -> Material.RED_DYE;
      case "green" -> Material.LIME_DYE;
      case "blue" -> Material.BLUE_DYE;
      case "yellow" -> Material.YELLOW_DYE;
      case "gold" -> Material.YELLOW_DYE;
      case "purple" -> Material.PURPLE_DYE;
      case "aqua" -> Material.CYAN_DYE;
      default -> Material.WHITE_DYE;
    };

    ItemBuilder builder = ItemBuilder.from(colorMaterial)
        .setName(LEGACY.serialize(Component.text(archetype.name(), NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false)));

    List<String> lore = new ArrayList<>();
    lore.add(LEGACY.serialize(Component.text("ID: " + archetype.id(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.text("Color: " + archetype.color(), NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.empty()));
    lore.add(LEGACY.serialize(Component.text("Click to edit name/color", NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.text("Shift+click to remove", NamedTextColor.RED)
        .decoration(TextDecoration.ITALIC, false)));
    builder.setLore(lore);

    final int archIndex = index;
    return builder.asGuiItem(event -> handleArchetypeClick(event, session, archIndex, player));
  }

  private GuiItem createPolicyItem(EditorSession session, String perkId, String policy, int index, Player player) {
    ItemBuilder builder = ItemBuilder.from(Material.PAPER)
        .setName(LEGACY.serialize(Component.text("Policy: " + perkId, NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)));

    List<String> lore = new ArrayList<>();
    lore.add(LEGACY.serialize(Component.text("Type: " + policy, NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.empty()));
    lore.add(LEGACY.serialize(Component.text("Click to cycle type", NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.text("Shift+click to remove", NamedTextColor.RED)
        .decoration(TextDecoration.ITALIC, false)));
    builder.setLore(lore);

    final int policyIndex = index;
    return builder.asGuiItem(event -> handlePolicyClick(event, session, perkId, policyIndex, player));
  }

  private void handleActionClick(InventoryClickEvent event, String action) {
    if (!(event.getWhoClicked() instanceof Player player)) return;

    SettingsEditSession editSession = editSessions.get(player.getUniqueId());
    if (editSession == null) return;

    EditorSession session = editSession.editorSession();
    EditorTree tree = session.tree();

    switch (action) {
      case "back" -> {
        if (mainEditor == null) {
          Mint.sendThemedMessage(player, "<error>Error: Main editor not available!");
          player.closeInventory();
          return;
        }
        editSessions.remove(player.getUniqueId());
        openGuis.remove(player.getUniqueId());
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> mainEditor.reopenFor(player));
      }

      case "add_archetype" -> {
        Mint.sendThemedMessage(player, "<accent>Type archetype data as 'ID Name Color' (e.g., 'xp XP Focus green'):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          String[] parts = input.split("\\s+", 3);
          if (parts.length >= 2) {
            session.saveSnapshot();
            String id = parts[0];
            String name = parts[1];
            String color = parts.length >= 3 ? parts[2] : "white";
            tree.archetypes().add(new EditorTree.EditorArchetype(id, name, color));
            Mint.sendThemedMessage(player, "<success>Added archetype: <secondary>" + name);
          } else {
            Mint.sendThemedMessage(player, "<error>Invalid format. Use: ID Name Color");
          }
          chatInputHandlers.remove(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session));
        });
      }

      case "add_policy" -> {
        Mint.sendThemedMessage(player, "<accent>Type perk policy as 'PERK_ID TYPE' (e.g., 'mining_speed MAX'):");
        Mint.sendThemedMessage(player, "<neutral>Types: MAX, ADDITIVE");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          String[] parts = input.split("\\s+", 2);
          if (parts.length == 2) {
            String perkId = parts[0];
            String type = parts[1].toUpperCase();
            if (type.equals("MAX") || type.equals("ADDITIVE")) {
              session.saveSnapshot();
              tree.perkPolicies().put(perkId, type);
              Mint.sendThemedMessage(player, "<success>Added policy: <secondary>" + perkId + " -> " + type);
            } else {
              Mint.sendThemedMessage(player, "<error>Invalid type. Use MAX or ADDITIVE");
            }
          } else {
            Mint.sendThemedMessage(player, "<error>Invalid format. Use: PERK_ID TYPE");
          }
          chatInputHandlers.remove(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session));
        });
      }
    }

    event.setCancelled(true);
  }

  private void handlePropertyClick(InventoryClickEvent event, String action) {
    if (!(event.getWhoClicked() instanceof Player player)) return;

    SettingsEditSession editSession = editSessions.get(player.getUniqueId());
    if (editSession == null) return;

    EditorSession session = editSession.editorSession();
    EditorTree tree = session.tree();
    ClickType click = event.getClick();

    switch (action) {
      case "display_name" -> {
        Mint.sendThemedMessage(player, "<accent>Type the new display name in chat (or 'cancel' to abort):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if (!"cancel".equalsIgnoreCase(input)) {
            session.saveSnapshot();
            tree.setDisplayName(input);
            Mint.sendThemedMessage(player, "<success>Display name set to: <secondary>" + input);
          }
          chatInputHandlers.remove(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session));
        });
      }

      case "skill_points" -> {
        int delta = click.isShiftClick() ? 5 : 1;
        if (click.isRightClick()) delta = -delta;

        session.saveSnapshot();
        tree.setSkillPointsPerLevel(Math.max(1, tree.skillPointsPerLevel() + delta));
        refreshGui(player, session);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }

      case "root_node" -> {
        Mint.sendThemedMessage(player, "<info>Current root: <secondary>" + tree.rootNodeId());
        Mint.sendThemedMessage(player, "<neutral>Type new root node ID (or 'cancel' to abort):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if (!"cancel".equalsIgnoreCase(input)) {
            if (tree.getNode(input).isPresent()) {
              session.saveSnapshot();
              tree.setRootNodeId(input);
              Mint.sendThemedMessage(player, "<success>Root node set to: <secondary>" + input);
            } else {
              Mint.sendThemedMessage(player, "<error>Node not found: " + input);
            }
          }
          chatInputHandlers.remove(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session));
        });
      }
    }

    event.setCancelled(true);
  }

  private void handleArchetypeClick(InventoryClickEvent event, EditorSession session, int index, Player player) {
    var click = event.getClick();
    EditorTree tree = session.tree();

    if (index >= tree.archetypes().size()) return;

    if (click.isShiftClick()) {
      // Remove archetype
      session.saveSnapshot();
      EditorTree.EditorArchetype removed = tree.archetypes().remove(index);
      Mint.sendThemedMessage(player, "<accent>Removed archetype: <secondary>" + removed.name());
      refreshGui(player, session);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
    } else {
      // Cycle color
      String[] colors = {"white", "red", "green", "blue", "yellow", "gold", "purple", "aqua"};
      EditorTree.EditorArchetype arch = tree.archetypes().get(index);
      int colorIdx = -1;
      for (int i = 0; i < colors.length; i++) {
        if (colors[i].equals(arch.color())) {
          colorIdx = i;
          break;
        }
      }
      String newColor = colors[(colorIdx + 1) % colors.length];
      session.saveSnapshot();
      tree.archetypes().set(index, new EditorTree.EditorArchetype(arch.id(), arch.name(), newColor));
      refreshGui(player, session);
      Mint.sendThemedMessage(player, "<success>Color changed to: <secondary>" + newColor);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    event.setCancelled(true);
  }

  private void handlePolicyClick(InventoryClickEvent event, EditorSession session, String perkId, int index, Player player) {
    var click = event.getClick();
    EditorTree tree = session.tree();

    if (click.isShiftClick()) {
      // Remove policy
      session.saveSnapshot();
      tree.perkPolicies().remove(perkId);
      Mint.sendThemedMessage(player, "<accent>Removed policy: <secondary>" + perkId);
      refreshGui(player, session);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
    } else {
      // Cycle policy type
      String current = tree.perkPolicies().get(perkId);
      String newType = "MAX".equals(current) ? "ADDITIVE" : "MAX";
      session.saveSnapshot();
      tree.perkPolicies().put(perkId, newType);
      refreshGui(player, session);
      Mint.sendThemedMessage(player, "<success>Policy type changed to: <secondary>" + newType);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    event.setCancelled(true);
  }

  private void refreshGui(Player player, EditorSession session) {
    Gui gui = openGuis.get(player.getUniqueId());
    if (gui != null) {
      renderGui(gui, session, player);
      gui.update();
    }
  }

  private void handleChatEvent(org.bukkit.event.player.AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    ChatInputHandler handler = chatInputHandlers.get(player.getUniqueId());
    if (handler != null) {
      event.setCancelled(true);
      String message = event.getMessage();
      Bukkit.getScheduler().runTask(plugin, () -> handler.handle(message));
    }
  }

  private void handleClose(Player player) {
    UUID playerId = player.getUniqueId();

    // If there's a chat handler waiting, don't do anything (they closed for input)
    if (chatInputHandlers.containsKey(playerId)) {
      return;
    }

    SettingsEditSession session = editSessions.remove(playerId);
    openGuis.remove(playerId);
    if (session == null) return;

    // Reopen main tree editor
    if (mainEditor != null) {
      Bukkit.getScheduler().runTask(plugin, () -> mainEditor.reopenFor(player));
    }
  }

  /**
   * Clean up all session data when player quits to prevent memory leaks.
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();

    // Clean up all maps
    editSessions.remove(playerId);
    chatInputHandlers.remove(playerId);
    openGuis.remove(playerId);
  }
}
