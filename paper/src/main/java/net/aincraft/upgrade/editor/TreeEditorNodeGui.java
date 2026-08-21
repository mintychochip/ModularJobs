package net.aincraft.upgrade.editor;

import dev.craftux.api.inventory.ClickKind;
import dev.craftux.api.inventory.InteractionPolicy;
import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.gui.craftux.CraftuxItems;
import net.aincraft.gui.craftux.CraftuxUiHost;
import net.aincraft.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Node property editor as a craftux inventory view.
 */
public final class TreeEditorNodeGui implements Listener {

  private static final int GUI_SIZE = 54;
  private static final String MENU_ID = "tree_editor_node";

  private final Plugin plugin;
  private final InventoryRuntime inventory;
  private TreeEditorGui mainEditor;

  private final Map<UUID, NodeEditSession> editSessions = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotActions = new HashMap<>();
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();
  private boolean chatListenerRegistered;

  private record NodeEditSession(EditorSession editorSession, EditorNode node) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(String input);
  }

  public TreeEditorNodeGui(Plugin plugin, InventoryRuntime inventory) {
    this.plugin = plugin;
    this.inventory = inventory;
  }

  public void setMainEditor(TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  public void open(@NotNull Player player, @NotNull EditorSession session, @NotNull EditorNode node) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, new NodeEditSession(session, node));
    ensureChatListener();
    inventory.open(playerId, buildView(player, session, node));
  }

  public void onAction(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    NodeEditSession edit = editSessions.get(audience);
    if (player == null || edit == null) {
      return;
    }
    Map<Integer, String> actions = slotActions.get(audience);
    if (actions == null) {
      return;
    }
    String action = actions.get(click.slot());
    if (action == null) {
      return;
    }
    handleAction(player, edit, action, click.policyKind());
  }

  InventoryView buildView(Player player, EditorSession session, EditorNode node) {
    final UUID audience = player.getUniqueId();
    Map<Integer, String> actions = new HashMap<>();
    Map<Integer, Slot> content = new HashMap<>();

    put(content, actions, 0, Material.ARROW, "back", "Back", List.of("Return to tree editor"));
    Material icon = node.icon() != null ? node.icon() : Material.PAPER;
    content.put(4, Slot.decorative(CraftuxItems.of(icon, node.name(), List.of("ID: " + node.id()))));

    put(content, actions, 10, Material.NAME_TAG, "name", "Name", List.of("Current: " + node.name(), "Click to edit"));
    put(content, actions, 11, Material.WRITABLE_BOOK, "description", "Description",
        List.of("Current: " + (node.description() != null ? node.description() : "(none)"), "Click to edit"));
    put(content, actions, 12, icon, "icon", "Icon",
        List.of("Current: " + icon.name(), "Click to cycle materials"));
    put(content, actions, 13, Material.DIAMOND, "cost", "Cost (SP)",
        List.of("Current: " + node.cost(), "Left +1 | Right -1 | Shift ±5"));

    put(content, actions, 19, Material.ENCHANTED_BOOK, "perk_id", "Perk ID",
        List.of("Current: " + (node.perkId().isEmpty() ? "(none)" : node.perkId()), "Click to edit"));
    put(content, actions, 20, Material.EXPERIENCE_BOTTLE, "level", "Perk Level",
        List.of("Current: " + node.level(), "Left +1 | Right -1"));
    put(content, actions, 21, Material.PURPLE_DYE, "archetype", "Archetype",
        List.of("Current: " + (node.archetypeRef() != null ? node.archetypeRef() : "(none)"), "Click to cycle"));

    put(content, actions, 28, Material.BREWING_STAND, "add_effect", "Add Effect",
        List.of("Add a boost effect stub"));
    List<EditorEffect> effects = node.effects();
    for (int i = 0; i < Math.min(effects.size(), 7); i++) {
      EditorEffect effect = effects.get(i);
      put(content, actions, 29 + i, Material.GOLDEN_APPLE, "effect_" + i,
          "Effect: " + effect.type().name(),
          List.of(effect.getDisplayDescription(), "Shift+click to remove"));
    }

    put(content, actions, 37, Material.COMPASS, "position", "Position",
        List.of("X: " + (node.position() != null ? node.position().x() : 0)
            + ", Y: " + (node.position() != null ? node.position().y() : 0)));
    put(content, actions, 49, Material.BARRIER, "delete", "Delete Node",
        List.of("Permanently remove this node"));

    InventoryView.Builder builder = InventoryView.builder(MENU_ID, 6)
        .title(trim("Edit Node: " + node.name()))
        .interactionPolicy(new InteractionPolicy(
            EnumSet.of(ClickKind.LEFT, ClickKind.RIGHT, ClickKind.SHIFT_LEFT, ClickKind.SHIFT_RIGHT),
            true, true));
    for (int i = 0; i < GUI_SIZE; i++) {
      Slot slot = content.get(i);
      if (slot != null) {
        builder.slot(i, slot);
      } else {
        builder.decorative(i, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE));
      }
    }

    slotActions.put(audience, Map.copyOf(actions));
    return builder.build();
  }

  private void put(
      Map<Integer, Slot> content,
      Map<Integer, String> actions,
      int index,
      Material material,
      String action,
      String label,
      List<String> lore) {
    content.put(index, Slot.button(
        "act." + action,
        CraftuxItems.of(material, label, lore),
        CraftuxUiHost.ACTION_EDITOR_NODE_PROP,
        SlotPixelIntent.UNVALIDATED));
    actions.put(index, action);
  }

  private void handleAction(Player player, NodeEditSession edit, String action, ClickKind kind) {
    EditorNode node = edit.node();
    EditorSession session = edit.editorSession();
    session.saveSnapshot();

    switch (action) {
      case "back" -> {
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.reopenFor(player);
        }
      }
      case "name" -> prompt(player, "Enter new name:", input -> {
        node.setName(input);
        Messages.send(player, "<success>Name set to: <secondary>" + input);
        reopen(player, edit);
      });
      case "description" -> prompt(player, "Enter description:", input -> {
        node.setDescription(input);
        Messages.send(player, "<success>Description updated");
        reopen(player, edit);
      });
      case "icon" -> {
        Material[] cycle = {
            Material.PAPER, Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT,
            Material.IRON_INGOT, Material.BOOK, Material.BLAZE_POWDER, Material.ENCHANTED_BOOK
        };
        Material current = node.icon() != null ? node.icon() : Material.PAPER;
        int idx = 0;
        for (int i = 0; i < cycle.length; i++) {
          if (cycle[i] == current) {
            idx = i;
            break;
          }
        }
        node.setIcon(cycle[(idx + 1) % cycle.length]);
        reopen(player, edit);
      }
      case "cost" -> {
        int delta = kind == ClickKind.SHIFT_LEFT || kind == ClickKind.SHIFT_RIGHT ? 5 : 1;
        if (kind == ClickKind.RIGHT || kind == ClickKind.SHIFT_RIGHT) {
          delta = -delta;
        }
        node.setCost(Math.max(0, node.cost() + delta));
        reopen(player, edit);
      }
      case "perk_id" -> prompt(player, "Enter perk id (or blank to clear):", input -> {
        node.setPerkId(input == null ? "" : input.trim());
        reopen(player, edit);
      });
      case "level" -> {
        int delta = kind == ClickKind.RIGHT ? -1 : 1;
        node.setLevel(Math.max(0, node.level() + delta));
        reopen(player, edit);
      }
      case "archetype" -> {
        // Cycle among free-form: clear / warrior / mage / assassin
        String[] cycle = {null, "warrior", "mage", "assassin"};
        String current = node.archetypeRef();
        int idx = 0;
        for (int i = 0; i < cycle.length; i++) {
          if ((cycle[i] == null && current == null)
              || (cycle[i] != null && cycle[i].equals(current))) {
            idx = i;
            break;
          }
        }
        node.setArchetypeRef(cycle[(idx + 1) % cycle.length]);
        reopen(player, edit);
      }
      case "add_effect" -> {
        EditorEffect effect = new EditorEffect();
        effect.setType(EditorEffect.EffectType.BOOST);
        effect.setTarget("xp");
        effect.setAmount(1.1);
        node.effects().add(effect);
        Messages.send(player, "<success>Added boost effect stub");
        reopen(player, edit);
      }
      case "position" -> prompt(player, "Enter position as x,y:", input -> {
        try {
          String[] parts = input.split(",");
          int x = Integer.parseInt(parts[0].trim());
          int y = Integer.parseInt(parts[1].trim());
          node.setPosition(new net.aincraft.upgrade.Position(x, y));
          Messages.send(player, "<success>Position set to " + x + "," + y);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
          Messages.send(player, "<error>Invalid position. Use x,y");
        }
        reopen(player, edit);
      });
      case "delete" -> {
        if (node.id().equals(session.tree().rootNodeId())) {
          Messages.send(player, "<error>Cannot delete root node!");
          return;
        }
        session.tree().removeNode(node.id());
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.reopenFor(player);
        }
      }
      default -> {
        if (action.startsWith("effect_")) {
          int index = Integer.parseInt(action.substring("effect_".length()));
          if (kind == ClickKind.SHIFT_LEFT || kind == ClickKind.SHIFT_RIGHT) {
            if (index >= 0 && index < node.effects().size()) {
              node.effects().remove(index);
              Messages.send(player, "<accent>Removed effect");
              reopen(player, edit);
            }
          }
        }
      }
    }
  }

  private void reopen(Player player, NodeEditSession edit) {
    inventory.open(player.getUniqueId(), buildView(player, edit.editorSession(), edit.node()));
  }

  private void prompt(Player player, String message, ChatInputHandler handler) {
    Messages.send(player, "<accent>" + message);
    chatInputHandlers.put(player.getUniqueId(), handler);
    inventory.close(player.getUniqueId());
  }

  private void ensureChatListener() {
    if (chatListenerRegistered) {
      return;
    }
    chatListenerRegistered = true;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onChat(AsyncPlayerChatEvent event) {
    ChatInputHandler handler = chatInputHandlers.remove(event.getPlayer().getUniqueId());
    if (handler == null) {
      return;
    }
    event.setCancelled(true);
    String input = event.getMessage();
    Bukkit.getScheduler().runTask(plugin, () -> handler.handle(input));
  }

  private static String trim(String title) {
    return title.length() > 128 ? title.substring(0, 128) : title;
  }
}
