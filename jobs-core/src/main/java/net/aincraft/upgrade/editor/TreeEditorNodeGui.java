package net.aincraft.upgrade.editor;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Sub-GUI for editing individual node properties using Triumph GUI.
 */
public final class TreeEditorNodeGui {

  private static final int GUI_SIZE = 54;
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  private final Plugin plugin;

  // Injected via setter to avoid circular dependency
  private TreeEditorGui mainEditor;

  // Active node edit sessions
  private final Map<UUID, NodeEditSession> editSessions = new HashMap<>();

  // Chat input listeners
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();

  // Track players who will reopen node editor (prevents close handler from interfering)
  private final Set<UUID> reopeningToNodeEditor = new HashSet<>();

  // Store open GUIs
  private final Map<UUID, Gui> openGuis = new HashMap<>();

  private record NodeEditSession(EditorSession editorSession, EditorNode node) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(String input);
  }

  @Inject
  public TreeEditorNodeGui(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Setter injection for TreeEditorGui to break circular dependency.
   * Guice calls this after both objects are constructed.
   */
  @Inject
  public void setMainEditor(TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  /**
   * Open the node editor for a specific node.
   */
  public void open(@NotNull Player player, @NotNull EditorSession session, @NotNull EditorNode node) {
    UUID playerId = player.getUniqueId();

    // Store edit session
    editSessions.put(playerId, new NodeEditSession(session, node));

    // Create and open GUI
    Gui gui = createGui(player, session, node);
    gui.open(player);
    openGuis.put(playerId, gui);
  }

  private Gui createGui(Player player, EditorSession session, EditorNode node) {
    Component title = Component.text()
        .append(Component.text("Edit Node: ", NamedTextColor.DARK_GRAY))
        .append(Component.text(node.name(), NamedTextColor.GOLD))
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

    renderGui(gui, node, session, player);

    return gui;
  }

  private void renderGui(Gui gui, EditorNode node, EditorSession session, Player player) {
    // Fill background
    GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
        .setName(" ")
        .asGuiItem();
    for (int i = 0; i < GUI_SIZE; i++) {
      gui.setItem(i, pane);
    }

    // Row 0: Back button and node info
    gui.setItem(0, createActionItem(Material.ARROW, "back", "Back", NamedTextColor.WHITE,
        "Return to tree editor"));

    // Node preview
    ItemStack preview = new ItemStack(node.icon());
    org.bukkit.inventory.meta.ItemMeta previewMeta = preview.getItemMeta();
    previewMeta.displayName(Component.text(node.name(), NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false));
    List<Component> previewLore = new ArrayList<>();
    previewLore.add(Component.text("ID: " + node.id(), NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.ITALIC, false));
    previewMeta.lore(previewLore);
    preview.setItemMeta(previewMeta);
    gui.setItem(4, ItemBuilder.from(preview).asGuiItem(event -> event.setCancelled(true)));

    // Row 1: Basic properties
    gui.setItem(10, createPropertyItem(Material.NAME_TAG, "name",
        "Name", node.name(), "Click to edit"));

    gui.setItem(11, createPropertyItem(Material.WRITABLE_BOOK, "description",
        "Description", node.description() != null ? node.description() : "(none)", "Click to edit"));

    gui.setItem(12, createPropertyItem(node.icon(), "icon",
        "Icon", node.icon().name(), "Click to cycle through materials"));

    gui.setItem(13, createPropertyItem(Material.DIAMOND, "cost",
        "Cost (SP)", String.valueOf(node.cost()),
        "Left-click: +1", "Right-click: -1", "Shift: +/-5"));

    // Row 2: Perk properties
    gui.setItem(19, createPropertyItem(Material.ENCHANTED_BOOK, "perk_id",
        "Perk ID", node.perkId().isEmpty() ? "(none)" : node.perkId(), "Click to edit"));

    gui.setItem(20, createPropertyItem(Material.EXPERIENCE_BOTTLE, "level",
        "Perk Level", String.valueOf(node.level()),
        "Left-click: +1", "Right-click: -1"));

    gui.setItem(21, createPropertyItem(Material.PURPLE_DYE, "archetype",
        "Archetype", node.archetypeRef() != null ? node.archetypeRef() : "(none)", "Click to cycle"));

    // Row 3: Effects
    gui.setItem(28, createActionItem(Material.BREWING_STAND, "add_effect",
        "Add Effect", NamedTextColor.GREEN, "Add a new effect to this node"));

    // Show existing effects
    List<EditorEffect> effects = node.effects();
    for (int i = 0; i < Math.min(effects.size(), 7); i++) {
      EditorEffect effect = effects.get(i);
      gui.setItem(29 + i, createEffectItem(session, node, effect, i, player));
    }

    // Row 4: Position
    gui.setItem(37, createPropertyItem(Material.COMPASS, "position",
        "Position", "X: " + (node.position() != null ? node.position().x() : 0) +
            ", Y: " + (node.position() != null ? node.position().y() : 0),
        "Click to edit coordinates"));

    // Row 5: Delete button
    gui.setItem(49, createActionItem(Material.BARRIER, "delete",
        "Delete Node", NamedTextColor.RED, "Permanently remove this node"));
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

  private GuiItem createEffectItem(EditorSession session, EditorNode node, EditorEffect effect, int index, Player player) {
    Material material = switch (effect.type()) {
      case BOOST -> Material.GOLDEN_APPLE;
      case PASSIVE -> Material.BLAZE_POWDER;
      case PERMISSION -> Material.PAPER;
      case RULED_BOOST -> Material.ENCHANTED_GOLDEN_APPLE;
    };

    ItemBuilder builder = ItemBuilder.from(material)
        .setName(LEGACY.serialize(Component.text("Effect: " + effect.type().name(), NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false)));

    List<String> lore = new ArrayList<>();
    lore.add(LEGACY.serialize(Component.text(effect.getDisplayDescription(), NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.empty()));
    lore.add(LEGACY.serialize(Component.text("Click to edit", NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false)));
    lore.add(LEGACY.serialize(Component.text("Shift+click to remove", NamedTextColor.RED)
        .decoration(TextDecoration.ITALIC, false)));
    builder.setLore(lore);

    final int effectIndex = index;
    return builder.asGuiItem(event -> handleEffectClick(event, session, node, effect, effectIndex, player));
  }

  private void handleActionClick(InventoryClickEvent event, String action) {
    if (!(event.getWhoClicked() instanceof Player player)) return;

    NodeEditSession editSession = editSessions.get(player.getUniqueId());
    if (editSession == null) return;

    EditorNode node = editSession.node();
    EditorSession session = editSession.editorSession();

    switch (action) {
      case "back" -> {
        // Return to main editor
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

      case "add_effect" -> {
        session.saveSnapshot();
        EditorEffect newEffect = new EditorEffect();
        node.effects().add(newEffect);
        refreshGui(player, session, node);
        Mint.sendThemedMessage(player, "<success>Added new effect");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
      }

      case "delete" -> {
        EditorTree tree = session.tree();
        if (node.id().equals(tree.rootNodeId())) {
          Mint.sendThemedMessage(player, "<error>Cannot delete root node!");
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
          return;
        }

        session.saveSnapshot();
        tree.removeNode(node.id());
        Mint.sendThemedMessage(player, "<accent>Deleted node: <secondary>" + node.id());
        editSessions.remove(player.getUniqueId());
        openGuis.remove(player.getUniqueId());
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
      }
    }

    event.setCancelled(true);
  }

  private void handlePropertyClick(InventoryClickEvent event, String action) {
    if (!(event.getWhoClicked() instanceof Player player)) return;

    NodeEditSession editSession = editSessions.get(player.getUniqueId());
    if (editSession == null) return;

    EditorNode node = editSession.node();
    EditorSession session = editSession.editorSession();
    var click = event.getClick();

    switch (action) {
      case "name" -> {
        Mint.sendThemedMessage(player, "<accent>Type the new name in chat (or 'cancel' to abort):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if (!"cancel".equalsIgnoreCase(input)) {
            session.saveSnapshot();
            node.setName(input);
            Mint.sendThemedMessage(player, "<success>Name set to: <secondary>" + input);
          }
          chatInputHandlers.remove(player.getUniqueId());
          reopeningToNodeEditor.add(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session, node));
        });
      }

      case "description" -> {
        Mint.sendThemedMessage(player, "<accent>Type the new description in chat (or 'cancel' to abort):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if (!"cancel".equalsIgnoreCase(input)) {
            session.saveSnapshot();
            node.setDescription(input);
            Mint.sendThemedMessage(player, "<success>Description set!");
          }
          chatInputHandlers.remove(player.getUniqueId());
          reopeningToNodeEditor.add(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session, node));
        });
      }

      case "icon" -> {
        Material[] materials = {
            Material.STONE, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
            Material.EMERALD, Material.WOODEN_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.BOOK, Material.ENCHANTED_BOOK, Material.EXPERIENCE_BOTTLE, Material.BLAZE_POWDER
        };
        int currentIndex = -1;
        for (int i = 0; i < materials.length; i++) {
          if (materials[i] == node.icon()) {
            currentIndex = i;
            break;
          }
        }
        int nextIndex = click.isRightClick() ?
            (currentIndex - 1 + materials.length) % materials.length :
            (currentIndex + 1) % materials.length;

        session.saveSnapshot();
        node.setIcon(materials[nextIndex]);
        node.setUnlockedIcon(materials[nextIndex]);
        refreshGui(player, session, node);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }

      case "cost" -> {
        int delta = click.isShiftClick() ? 5 : 1;
        if (click.isRightClick()) delta = -delta;

        session.saveSnapshot();
        node.setCost(node.cost() + delta);
        refreshGui(player, session, node);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }

      case "perk_id" -> {
        Mint.sendThemedMessage(player, "<accent>Type the perk ID in chat (or 'cancel' to abort, 'clear' to remove):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if ("clear".equalsIgnoreCase(input)) {
            session.saveSnapshot();
            node.setPerkId("");
            Mint.sendThemedMessage(player, "<success>Perk ID cleared");
          } else if (!"cancel".equalsIgnoreCase(input)) {
            session.saveSnapshot();
            node.setPerkId(input);
            Mint.sendThemedMessage(player, "<success>Perk ID set to: <secondary>" + input);
          }
          chatInputHandlers.remove(player.getUniqueId());
          reopeningToNodeEditor.add(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session, node));
        });
      }

      case "level" -> {
        int delta = click.isRightClick() ? -1 : 1;
        session.saveSnapshot();
        node.setLevel(node.level() + delta);
        refreshGui(player, session, node);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }

      case "archetype" -> {
        List<EditorTree.EditorArchetype> archetypes = session.tree().archetypes();
        if (archetypes.isEmpty()) {
          Mint.sendThemedMessage(player, "<error>No archetypes defined!");
          return;
        }

        int currentIdx = -1;
        for (int i = 0; i < archetypes.size(); i++) {
          if (archetypes.get(i).id().equals(node.archetypeRef())) {
            currentIdx = i;
            break;
          }
        }
        int nextIdx = (currentIdx + 1) % (archetypes.size() + 1);

        session.saveSnapshot();
        if (nextIdx >= archetypes.size()) {
          node.setArchetypeRef(null);
        } else {
          node.setArchetypeRef(archetypes.get(nextIdx).id());
        }
        refreshGui(player, session, node);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }

      case "position" -> {
        Mint.sendThemedMessage(player, "<accent>Type new position as 'X Y' (e.g., '4 2'):");
        player.closeInventory();
        chatInputHandlers.put(player.getUniqueId(), input -> {
          if (!"cancel".equalsIgnoreCase(input)) {
            try {
              String[] parts = input.split("\\s+");
              int x = Integer.parseInt(parts[0]);
              int y = Integer.parseInt(parts[1]);
              session.saveSnapshot();
              node.setPosition(new net.aincraft.upgrade.Position(x, y));
              Mint.sendThemedMessage(player, "<success>Position set to: <secondary>" + x + ", " + y);
            } catch (Exception e) {
              Mint.sendThemedMessage(player, "<error>Invalid format. Use 'X Y' (e.g., '4 2')");
            }
          }
          chatInputHandlers.remove(player.getUniqueId());
          reopeningToNodeEditor.add(player.getUniqueId());
          Bukkit.getScheduler().runTask(plugin, () -> open(player, session, node));
        });
      }
    }

    event.setCancelled(true);
  }

  private void handleEffectClick(InventoryClickEvent event, EditorSession session, EditorNode node,
                                  EditorEffect effect, int index, Player player) {
    var click = event.getClick();

    if (click.isShiftClick()) {
      // Remove effect
      if (index < node.effects().size()) {
        session.saveSnapshot();
        node.effects().remove((int) index);
        refreshGui(player, session, node);
        Mint.sendThemedMessage(player, "<accent>Removed effect");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
      }
    } else {
      // Cycle effect type
      if (index < node.effects().size()) {
        session.saveSnapshot();
        EditorEffect.EffectType[] types = EditorEffect.EffectType.values();
        int typeIdx = effect.type().ordinal();
        effect.setType(types[(typeIdx + 1) % types.length]);
        refreshGui(player, session, node);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
      }
    }

    event.setCancelled(true);
  }

  private void refreshGui(Player player, EditorSession session, EditorNode node) {
    Gui gui = openGuis.get(player.getUniqueId());
    if (gui != null) {
      renderGui(gui, node, session, player);
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

    // If reopening to node editor, don't interfere
    if (reopeningToNodeEditor.remove(playerId)) {
      return;
    }

    NodeEditSession session = editSessions.remove(playerId);
    openGuis.remove(playerId);
    if (session == null) return;

    // Reopen main tree editor with a slight delay to avoid inventory event race conditions
    if (mainEditor != null) {
      Bukkit.getScheduler().runTaskLater(plugin, () -> mainEditor.reopenFor(player), 1L);
    }
  }
}
