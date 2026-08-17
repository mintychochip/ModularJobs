package net.aincraft.gui;

import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.ItemSpec;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.gui.craftux.CraftuxItems;
import net.aincraft.gui.craftux.CraftuxUiHost;
import net.aincraft.upgrade.NodeEffect;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.SkillNode;
import net.aincraft.upgrade.SkillTree;
import net.aincraft.upgrade.SkillTreeState;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.PurchaseResult;
import net.aincraft.upgrade.UpgradeService.UnlockResult;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Job upgrade tree GUI via craftux {@link InventoryRuntime}.
 *
 * <p>Presentation is rebuilt into an {@link InventoryView} on each open/refresh.
 * Click side-effects are host actions registered under {@link CraftuxUiHost}.
 */
public final class UpgradeTreeGui {

  private static final int GUI_SIZE = 54;
  private static final int GUI_ROWS = 5;
  private static final int GUI_COLS = 9;
  private static final int CONTROL_ROW_START = 45;
  private static final int CONFIRM_SLOT = 52;
  private static final String MENU_ID = "upgrade_tree";
  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  private final Plugin plugin;
  private final InventoryRuntime inventory;
  private final UpgradeService upgradeService;
  private final Map<UUID, GuiSession> openGuis = new HashMap<>();

  private static final class GuiSession {
    final Job job;
    final UpgradeTree tree;
    final SkillTree skillTree;
    String pendingMajorKey;
    int scrollOffset;
    /** Slot → node key for action dispatch. */
    Map<Integer, String> slotNodes = Map.of();

    GuiSession(Job job, UpgradeTree tree, SkillTree skillTree) {
      this.job = job;
      this.tree = tree;
      this.skillTree = skillTree;
      this.pendingMajorKey = null;
      this.scrollOffset = 0;
    }

    boolean isV2() {
      return skillTree != null;
    }
  }

  /** Builds the tree presenter over the shared craftux runtime. */
  public UpgradeTreeGui(Plugin plugin, InventoryRuntime inventory, UpgradeService upgradeService) {
    this.plugin = plugin;
    this.inventory = inventory;
    this.upgradeService = upgradeService;
  }

  /**
   * Opens (or replaces) the upgrade-tree view for {@code player}, resolving the
   * active skill tree and resetting scroll/pending state. Bukkit thread.
   */
  public void open(Player player, Job job, UpgradeTree tree) {
    String jobKey = job.key().value();
    SkillTree skillTree = upgradeService.getSkillTree(jobKey).orElse(null);
    if (tree == null && skillTree == null) {
      return;
    }

    UUID playerUuid = player.getUniqueId();
    GuiSession session = new GuiSession(job, tree, skillTree);
    openGuis.put(playerUuid, session);
    inventory.open(playerUuid, buildView(player, session));
  }

  /** Re-renders the caller's open tree view in place (Bukkit thread). */
  public void refresh(Player player) {
    UUID playerId = player.getUniqueId();
    GuiSession session = openGuis.get(playerId);
    if (session == null) {
      return;
    }
    inventory.refresh(playerId, buildView(player, session));
  }

  /**
   * Host action handler for {@link CraftuxUiHost#ACTION_UPGRADE_NODE}: unlocks/
   * purchases the clicked node, staging permanent "major" choices for confirm.
   * Runs on the Bukkit thread via the craftux runtime.
   */
  public void onNodeClick(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    GuiSession session = openGuis.get(audience);
    if (player == null || session == null) {
      return;
    }
    String nodeKey = session.slotNodes.get(click.slot());
    if (nodeKey == null) {
      return;
    }
    String playerId = audience.toString();
    String jobKey = session.job.key().value();
    if (session.isV2()) {
      handleV2NodeClick(player, session, nodeKey, playerId, jobKey);
    } else {
      handleLegacyUnlock(player, session, nodeKey, playerId, jobKey);
    }
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_UPGRADE_SCROLL_UP}. */
  public void onScrollUp(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    GuiSession session = openGuis.get(audience);
    if (player == null || session == null) {
      return;
    }
    handleScroll(player, session, "up");
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_UPGRADE_SCROLL_DOWN}. */
  public void onScrollDown(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    GuiSession session = openGuis.get(audience);
    if (player == null || session == null) {
      return;
    }
    handleScroll(player, session, "down");
  }

  /** Host action handler for {@link CraftuxUiHost#ACTION_UPGRADE_CONFIRM}. */
  public void onConfirm(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    GuiSession session = openGuis.get(audience);
    if (player == null || session == null || session.pendingMajorKey == null) {
      return;
    }
    purchaseMajor(player, session, session.pendingMajorKey);
  }

  InventoryView buildView(Player player, GuiSession session) {
    String playerId = player.getUniqueId().toString();
    String jobKey = session.job.key().value();
    PlayerUpgradeData data = loadData(playerId, jobKey, session);
    SkillTreeState state = loadState(playerId, jobKey, session);

    Map<Integer, Slot> slots = new HashMap<>();
    Map<Integer, String> slotNodes = new HashMap<>();

    ItemSpec bg = CraftuxItems.pane(Material.BLACK_STAINED_GLASS_PANE);
    for (int i = 0; i < CONTROL_ROW_START; i++) {
      slots.put(i, Slot.decorative(bg));
    }

    if (!session.isV2()) {
      renderConnections(slots, session, data.unlockedNodes(), session.tree.getAvailableNodes(data.unlockedNodes(), data));
      for (UpgradeNode node : session.tree.allNodes()) {
        int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
        if (slot < 0 || slot >= CONTROL_ROW_START) {
          continue;
        }
        NodeStatus status = getNodeStatus(node, data.unlockedNodes(), session.tree.getAvailableNodes(data.unlockedNodes(), data));
        String key = getShortKey(node);
        slots.put(slot, Slot.button(
            "node." + sanitize(key),
            nodeItem(node, status, data, session.tree),
            CraftuxUiHost.ACTION_UPGRADE_NODE,
            SlotPixelIntent.UNVALIDATED));
        slotNodes.put(slot, key);
      }
    } else {
      for (SkillNode node : session.skillTree.nodes()) {
        int slot = calculateSlotWithScroll(node.position(), session.scrollOffset);
        if (slot < 0 || slot >= CONTROL_ROW_START) {
          continue;
        }
        NodeStatus status = v2Status(session.skillTree, state, node);
        String key = node.key().value();
        slots.put(slot, Slot.button(
            "node." + sanitize(key),
            v2NodeItem(node, status, state, session.skillTree),
            CraftuxUiHost.ACTION_UPGRADE_NODE,
            SlotPixelIntent.UNVALIDATED));
        slotNodes.put(slot, key);
      }
    }

    placeControls(slots, session, data, state);

    session.slotNodes = Map.copyOf(slotNodes);

    String title = PLAIN.serialize(session.job.displayName()) + " Upgrades";
    if (title.length() > 128) {
      title = title.substring(0, 128);
    }

    InventoryView.Builder builder = InventoryView.builder(MENU_ID, 6).title(title);
    for (int i = 0; i < GUI_SIZE; i++) {
      Slot slot = slots.get(i);
      if (slot != null) {
        builder.slot(i, slot);
      } else {
        builder.decorative(i, CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE));
      }
    }
    return builder.build();
  }

  private void placeControls(
      Map<Integer, Slot> slots, GuiSession session, PlayerUpgradeData data, SkillTreeState state) {
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

    ItemSpec controlBg = CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = CONTROL_ROW_START; i < GUI_SIZE; i++) {
      slots.put(i, Slot.decorative(controlBg));
    }

    Material upMat = canScrollUp ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
    String upLabel = canScrollUp ? "Scroll Up" : "Scroll Up (At Top)";
    if (canScrollUp) {
      slots.put(CONTROL_ROW_START, Slot.navigation(
          "scroll_up",
          CraftuxItems.of(upMat, upLabel),
          CraftuxUiHost.ACTION_UPGRADE_SCROLL_UP,
          SlotPixelIntent.UNVALIDATED));
    } else {
      slots.put(CONTROL_ROW_START, Slot.decorative(CraftuxItems.of(upMat, upLabel)));
    }

    Material downMat = canScrollDown ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
    String downLabel = canScrollDown ? "Scroll Down" : "Scroll Down (At Bottom)";
    if (canScrollDown) {
      slots.put(CONTROL_ROW_START + 8, Slot.navigation(
          "scroll_down",
          CraftuxItems.of(downMat, downLabel),
          CraftuxUiHost.ACTION_UPGRADE_SCROLL_DOWN,
          SlotPixelIntent.UNVALIDATED));
    } else {
      slots.put(CONTROL_ROW_START + 8, Slot.decorative(CraftuxItems.of(downMat, downLabel)));
    }

    slots.put(CONTROL_ROW_START + 4, Slot.decorative(
        session.isV2() ? v2InfoItem(session.skillTree, state) : infoItem(session.job, session.tree, data)));

    if (session.pendingMajorKey != null) {
      String pendingName = session.skillTree.node(session.pendingMajorKey)
          .map(SkillNode::name).orElse(session.pendingMajorKey);
      List<String> lore = List.of(
          pendingName,
          "Permanent choice - cannot be refunded",
          "Click to confirm");
      slots.put(CONFIRM_SLOT, Slot.button(
          "confirm_major",
          CraftuxItems.of(Material.GOLD_INGOT, "Confirm Major?", lore),
          CraftuxUiHost.ACTION_UPGRADE_CONFIRM,
          SlotPixelIntent.UNVALIDATED));
    }
  }

  private void renderConnections(
      Map<Integer, Slot> slots,
      GuiSession session,
      Set<String> unlocked,
      Set<UpgradeNode> available) {
    int scrollOffset = session.scrollOffset;
    Set<GridPoint> allPathPoints = new HashSet<>();
    for (Position p : session.tree.paths()) {
      allPathPoints.add(new GridPoint(p.x(), p.y()));
    }

    Set<GridPoint> unlockedNodePositions = new HashSet<>();
    for (UpgradeNode node : session.tree.allNodes()) {
      if (node.position() == null) {
        continue;
      }
      GridPoint point = new GridPoint(node.position().x(), node.position().y());
      if (unlocked.contains(getShortKey(node))) {
        unlockedNodePositions.add(point);
      }
    }

    Set<GridPoint> litPathPoints = new HashSet<>();
    int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
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
          if (visited.contains(neighbor)) {
            continue;
          }
          visited.add(neighbor);
          parent.put(neighbor, current);
          if (unlockedNodePositions.contains(neighbor)) {
            GridPoint trace = neighbor;
            while (trace != null && parent.containsKey(trace)) {
              GridPoint prev = parent.get(trace);
              if (allPathPoints.contains(trace)) {
                litPathPoints.add(trace);
              }
              trace = prev;
            }
            queue.add(neighbor);
          } else if (allPathPoints.contains(neighbor)) {
            queue.add(neighbor);
          }
        }
      }
    }

    for (GridPoint pathPoint : allPathPoints) {
      int screenY = pathPoint.y - scrollOffset;
      if (screenY < 0 || screenY >= GUI_ROWS || pathPoint.x < 0 || pathPoint.x >= GUI_COLS) {
        continue;
      }
      boolean isLit = litPathPoints.contains(pathPoint);
      Material material = isLit ? Material.CYAN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
      int slot = screenY * GUI_COLS + pathPoint.x;
      if (slot >= 0 && slot < CONTROL_ROW_START) {
        slots.put(slot, Slot.decorative(CraftuxItems.pane(material)));
      }
    }
  }

  private PlayerUpgradeData loadData(String playerId, String jobKey, GuiSession session) {
    return session.isV2() ? null : upgradeService.getPlayerData(playerId, jobKey);
  }

  private SkillTreeState loadState(String playerId, String jobKey, GuiSession session) {
    return session.isV2() ? upgradeService.getSkillTreeState(playerId, jobKey) : null;
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

  private NodeStatus getNodeStatus(UpgradeNode node, Set<String> unlocked, Set<UpgradeNode> available) {
    String shortKey = getShortKey(node);
    if (unlocked.contains(shortKey)) {
      return NodeStatus.UNLOCKED;
    }
    for (String exclusiveKey : node.exclusive()) {
      if (unlocked.contains(exclusiveKey)) {
        return NodeStatus.EXCLUDED;
      }
    }
    if (available.contains(node)) {
      return NodeStatus.AVAILABLE;
    }
    return NodeStatus.LOCKED;
  }

  private int calculateSlotWithScroll(Position position, int scrollOffset) {
    if (position == null) {
      return -1;
    }
    int x = position.x();
    int y = position.y() - scrollOffset;
    if (x < 0 || x >= GUI_COLS || y < 0 || y >= GUI_ROWS) {
      return -1;
    }
    return y * GUI_COLS + x;
  }

  private ItemSpec nodeItem(UpgradeNode node, NodeStatus status, PlayerUpgradeData data, UpgradeTree tree) {
    Material material = switch (status) {
      case UNLOCKED -> materialFromName(node.unlockedIcon());
      case AVAILABLE -> materialFromName(node.icon());
      case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
      case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
    };
    NamedTextColor nameColor = switch (status) {
      case UNLOCKED -> NamedTextColor.GREEN;
      case AVAILABLE -> NamedTextColor.YELLOW;
      case LOCKED -> NamedTextColor.GRAY;
      case EXCLUDED -> NamedTextColor.RED;
    };
    List<String> lore = new ArrayList<>();
    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(line);
      }
    }
    lore.add("");
    lore.add("Cost: " + node.cost() + " SP");
    if (!node.effects().isEmpty()) {
      lore.add("");
      lore.add("Effects:");
      for (UpgradeEffect effect : node.effects()) {
        lore.add("  • " + formatEffect(effect));
      }
    }
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add("");
      lore.add("Requires:");
      for (String prereq : node.prerequisites()) {
        lore.add("  • " + prereq);
      }
    }
    lore.add("");
    lore.add(switch (status) {
      case UNLOCKED -> "✔ Unlocked!";
      case AVAILABLE -> data.availableSkillPoints() >= node.cost() ? "Click to unlock" : "Not enough SP!";
      case LOCKED -> "Locked";
      case EXCLUDED -> "✘ Path Locked (Exclusive Choice)";
    });
    if (status == NodeStatus.EXCLUDED && !node.exclusive().isEmpty()) {
      List<String> exclusiveNames = node.exclusive().stream()
          .map(tree::getNode)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(UpgradeNode::name)
          .toList();
      lore.add("Blocked by: " + String.join(", ", exclusiveNames));
    }
    return CraftuxItems.of(material, plain(Component.text(node.name(), nameColor)
        .decoration(TextDecoration.ITALIC, false)), lore);
  }

  private ItemSpec v2NodeItem(SkillNode node, NodeStatus status, SkillTreeState state, SkillTree tree) {
    String key = node.key().value();
    int owned = state.levelOf(key);
    Material material = switch (status) {
      case UNLOCKED -> materialFromName(node.unlockedIcon());
      case AVAILABLE -> materialFromName(node.lockedIcon());
      case LOCKED -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
      case EXCLUDED -> Material.RED_STAINED_GLASS_PANE;
    };
    NamedTextColor nameColor = switch (status) {
      case UNLOCKED -> NamedTextColor.GREEN;
      case AVAILABLE -> NamedTextColor.YELLOW;
      case LOCKED -> NamedTextColor.GRAY;
      case EXCLUDED -> NamedTextColor.RED;
    };
    List<String> lore = new ArrayList<>();
    if (node.description() != null && !node.description().isEmpty()) {
      for (String line : node.description().split("\n")) {
        lore.add(line);
      }
      lore.add("");
    }
    if (node.isSkill()) {
      lore.add("Level: " + owned + "/" + node.maxLevel());
    } else {
      lore.add("Permanent choice");
    }
    int cost = node.isSkill() ? node.levelCost(owned + 1) : node.cost();
    lore.add("Cost: " + cost + " SP");
    if (status == NodeStatus.UNLOCKED && !node.activeEffects(owned).isEmpty()) {
      lore.add("");
      lore.add("Active Effects:");
      for (NodeEffect effect : node.activeEffects(owned)) {
        lore.add("  • " + formatV2Effect(effect));
      }
    }
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      lore.add("");
      lore.add("Requires:");
      for (String prereq : node.prerequisites()) {
        lore.add("  • " + prereq);
      }
    }
    lore.add("");
    if (status == NodeStatus.UNLOCKED) {
      lore.add("✔ Unlocked!");
    } else if (status == NodeStatus.AVAILABLE) {
      if (node.isMajor()) {
        lore.add("Click to confirm - permanent choice");
      } else if (cost > tree.availablePoints(state)) {
        lore.add("Not enough SP!");
      } else {
        lore.add("Click to unlock");
      }
    } else if (status == NodeStatus.EXCLUDED) {
      lore.add("✘ Path Locked (Exclusive Choice)");
    } else {
      lore.add("Locked");
    }
    return CraftuxItems.of(material, plain(Component.text(node.name(), nameColor)
        .decoration(TextDecoration.ITALIC, false)), lore);
  }

  private ItemSpec infoItem(Job job, UpgradeTree tree, PlayerUpgradeData data) {
    List<String> lore = List.of(
        "Available SP: " + data.availableSkillPoints(),
        "Total SP: " + data.totalSkillPoints(),
        "Unlocked: " + data.unlockedNodes().size() + "/" + tree.allNodes().size(),
        "",
        "SP per level: " + tree.skillPointsPerLevel());
    return CraftuxItems.of(Material.BOOK, "Skill Tree Info", lore);
  }

  private ItemSpec v2InfoItem(SkillTree tree, SkillTreeState state) {
    long unlockedCount = state.nodeLevels().values().stream().filter(l -> l > 0).count();
    List<String> lore = List.of(
        "Available SP: " + tree.availablePoints(state),
        "Total SP: " + state.totalSkillPoints(),
        "Unlocked: " + unlockedCount + "/" + tree.nodes().size(),
        "",
        "Job Level: " + state.jobLevel(),
        "SP per level: " + tree.skillPointsPerLevel());
    return CraftuxItems.of(Material.BOOK, "Skill Tree Info", lore);
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
      case NodeEffect.CapabilityEffect capability ->
          String.format("Capability: %s (schema %d)", capability.key().asString(), capability.schema());
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

  private void handleV2NodeClick(Player player, GuiSession session, String nodeKey, String playerId, String jobKey) {
    SkillNode node = session.skillTree.node(nodeKey).orElse(null);
    if (node != null && node.isMajor()) {
      if (session.pendingMajorKey != null && !session.pendingMajorKey.equals(nodeKey)) {
        session.pendingMajorKey = null;
        refresh(player);
      }
      if (session.pendingMajorKey == null) {
        SkillTreeState state = upgradeService.getSkillTreeState(playerId, jobKey);
        if (session.skillTree.canPurchase(state, nodeKey)) {
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
      refresh(player);
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
      case UnlockResult.AlreadyUnlocked au ->
          player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
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

  private void handleScroll(Player player, GuiSession session, String action) {
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

    if ("up".equals(action) && session.scrollOffset > 0) {
      session.scrollOffset = Math.max(0, session.scrollOffset - GUI_ROWS);
    } else if ("down".equals(action) && session.scrollOffset < maxScroll) {
      session.scrollOffset = Math.min(maxScroll, session.scrollOffset + GUI_ROWS);
    } else {
      return;
    }
    refresh(player);
  }

  private static String sanitize(String key) {
    return key.replace(':', '.').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
  }

  private static String plain(Component component) {
    return PLAIN.serialize(component);
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

  private static final class GridPoint {
    final int x, y;

    GridPoint(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof GridPoint that)) {
        return false;
      }
      return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
      return 31 * x + y;
    }
  }
}
