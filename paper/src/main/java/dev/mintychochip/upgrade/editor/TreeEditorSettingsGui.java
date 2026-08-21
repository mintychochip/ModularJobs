package dev.mintychochip.upgrade.editor;

import dev.craftux.api.inventory.ClickKind;
import dev.craftux.api.inventory.InteractionPolicy;
import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import dev.mintychochip.gui.craftux.CraftuxItems;
import dev.mintychochip.gui.craftux.CraftuxUiHost;
import dev.mintychochip.util.Messages;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Tree-level settings editor via craftux inventory. */
public final class TreeEditorSettingsGui implements Listener {

  private static final int GUI_SIZE = 54;
  private static final String MENU_ID = "tree_editor_settings";

  private final Plugin plugin;
  private final InventoryRuntime inventory;
  private TreeEditorGui mainEditor;

  private final Map<UUID, SettingsEditSession> editSessions = new HashMap<>();
  private final Map<UUID, Map<Integer, String>> slotActions = new HashMap<>();
  private final Map<UUID, ChatInputHandler> chatInputHandlers = new HashMap<>();
  private boolean chatListenerRegistered;

  private record SettingsEditSession(EditorSession editorSession) {}

  @FunctionalInterface
  private interface ChatInputHandler {
    void handle(String input);
  }

  /** Tree editor settings gui. */
  public TreeEditorSettingsGui(Plugin plugin, InventoryRuntime inventory) {
    this.plugin = plugin;
    this.inventory = inventory;
  }

  public void setMainEditor(TreeEditorGui mainEditor) {
    this.mainEditor = mainEditor;
  }

  /** Open. */
  public void open(@NotNull Player player, @NotNull EditorSession session) {
    UUID playerId = player.getUniqueId();
    editSessions.put(playerId, new SettingsEditSession(session));
    ensureChatListener();
    inventory.open(playerId, buildView(player, session));
  }

  /** On action. */
  public void onAction(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    SettingsEditSession edit = editSessions.get(audience);
    if (player == null || edit == null) {
      return;
    }
    Map<Integer, String> actions = slotActions.get(audience);
    if (actions == null) {
      return;
    }
    String action = actions.get(click.slot());
    if (action != null) {
      handleAction(player, edit, action, click.policyKind());
    }
  }

  InventoryView buildView(Player player, EditorSession session) {
    final UUID audience = player.getUniqueId();
    EditorTree tree = session.tree();
    Map<Integer, String> actions = new HashMap<>();
    Map<Integer, Slot> content = new HashMap<>();

    put(content, actions, 0, Material.ARROW, "back", "Back", List.of("Return to tree editor"));
    content.put(
        4,
        Slot.decorative(
            CraftuxItems.of(
                Material.OAK_SIGN,
                tree.treeId(),
                List.of("Job: " + tree.jobKey(), "Nodes: " + tree.nodes().size()))));

    put(
        content,
        actions,
        10,
        Material.NAME_TAG,
        "display_name",
        "Display Name",
        List.of("Current: " + tree.displayName(), "Click to edit"));
    put(
        content,
        actions,
        11,
        Material.PAPER,
        "tree_id",
        "Tree ID",
        List.of("Current: " + tree.treeId(), "Click to edit"));
    put(
        content,
        actions,
        12,
        Material.EXPERIENCE_BOTTLE,
        "sp_per_level",
        "SP per Level",
        List.of("Current: " + tree.skillPointsPerLevel(), "Left +1 | Right -1"));

    put(
        content,
        actions,
        19,
        Material.BOOK,
        "job_key",
        "Job Key",
        List.of("Current: " + tree.jobKey(), "Click to edit"));

    InventoryView.Builder builder =
        InventoryView.builder(MENU_ID, 6)
            .title(trim("Tree Settings: " + tree.displayName()))
            .interactionPolicy(
                new InteractionPolicy(
                    EnumSet.of(
                        ClickKind.LEFT,
                        ClickKind.RIGHT,
                        ClickKind.SHIFT_LEFT,
                        ClickKind.SHIFT_RIGHT),
                    true,
                    true));
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
    content.put(
        index,
        Slot.button(
            "set." + action,
            CraftuxItems.of(material, label, lore),
            CraftuxUiHost.ACTION_EDITOR_SETTINGS,
            SlotPixelIntent.UNVALIDATED));
    actions.put(index, action);
  }

  private void handleAction(
      Player player, SettingsEditSession edit, String action, ClickKind kind) {
    EditorTree tree = edit.editorSession().tree();
    edit.editorSession().saveSnapshot();

    switch (action) {
      case "back" -> {
        editSessions.remove(player.getUniqueId());
        if (mainEditor != null) {
          mainEditor.reopenFor(player);
        }
      }
      case "display_name" ->
          prompt(
              player,
              "Enter display name:",
              input -> {
                tree.setDisplayName(input);
                reopen(player, edit);
              });
      case "tree_id" ->
          prompt(
              player,
              "Enter tree id:",
              input -> {
                tree.setTreeId(input.trim());
                reopen(player, edit);
              });
      case "job_key" ->
          prompt(
              player,
              "Enter job key:",
              input -> {
                tree.setJobKey(input.trim());
                reopen(player, edit);
              });
      case "sp_per_level" -> {
        int delta = kind == ClickKind.RIGHT || kind == ClickKind.SHIFT_RIGHT ? -1 : 1;
        tree.setSkillPointsPerLevel(Math.max(0, tree.skillPointsPerLevel() + delta));
        reopen(player, edit);
      }
      default -> {}
    }
  }

  private void reopen(Player player, SettingsEditSession edit) {
    inventory.open(player.getUniqueId(), buildView(player, edit.editorSession()));
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

  /** API member. */
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
