package dev.mintychochip.gui.craftux;

import dev.craftux.common.inventory.InventoryRuntime;
import dev.craftux.paper.inventory.BukkitInventoryPort;
import dev.craftux.paper.inventory.PaperInventoryRenderer;
import java.util.List;
import org.bukkit.plugin.Plugin;

/**
 * Composition-root craftux inventory host for ModularJobs GUIs.
 *
 * <p>Owns the shared {@link InventoryRuntime} and Paper click bridge. Individual GUIs build {@link
 * dev.craftux.api.inventory.InventoryView}s and open them via {@link #inventory()}; host actions
 * are registered through {@link #actions()}.
 */
public final class CraftuxUiHost {

  /** Job browse: click a job to join. */
  public static final String ACTION_JOB_JOIN = "modularjobs.jobs.join";

  /** Upgrade tree: click a node. */
  public static final String ACTION_UPGRADE_NODE = "modularjobs.upgrades.node";

  /** Upgrade tree: scroll up. */
  public static final String ACTION_UPGRADE_SCROLL_UP = "modularjobs.upgrades.scroll_up";

  /** Upgrade tree: scroll down. */
  public static final String ACTION_UPGRADE_SCROLL_DOWN = "modularjobs.upgrades.scroll_down";

  /** Upgrade tree: confirm major purchase. */
  public static final String ACTION_UPGRADE_CONFIRM = "modularjobs.upgrades.confirm";

  /** Tree editor canvas empty slot. */
  public static final String ACTION_EDITOR_CANVAS = "modularjobs.editor.canvas";

  /** Tree editor node click. */
  public static final String ACTION_EDITOR_NODE = "modularjobs.editor.node";

  /** Tree editor control (save/settings/…). */
  public static final String ACTION_EDITOR_CONTROL = "modularjobs.editor.control";

  /** Tree editor node property GUI. */
  public static final String ACTION_EDITOR_NODE_PROP = "modularjobs.editor.node_prop";

  /** Tree editor settings GUI. */
  public static final String ACTION_EDITOR_SETTINGS = "modularjobs.editor.settings";

  /** Stats inventory navigation. */
  public static final String ACTION_STATS_PREV = "modularjobs.stats.prev";

  public static final String ACTION_STATS_NEXT = "modularjobs.stats.next";

  /** Job info inventory navigation. */
  public static final String ACTION_INFO_PREV = "modularjobs.info.prev";

  public static final String ACTION_INFO_NEXT = "modularjobs.info.next";

  private static final List<String> ALL_ACTIONS =
      List.of(
          ACTION_JOB_JOIN,
          ACTION_UPGRADE_NODE,
          ACTION_UPGRADE_SCROLL_UP,
          ACTION_UPGRADE_SCROLL_DOWN,
          ACTION_UPGRADE_CONFIRM,
          ACTION_EDITOR_CANVAS,
          ACTION_EDITOR_NODE,
          ACTION_EDITOR_CONTROL,
          ACTION_EDITOR_NODE_PROP,
          ACTION_EDITOR_SETTINGS,
          ACTION_STATS_PREV,
          ACTION_STATS_NEXT,
          ACTION_INFO_PREV,
          ACTION_INFO_NEXT);

  private final CraftuxActionBus actions;
  private final BukkitInventoryPort port;
  private final InventoryRuntime inventory;
  private final PaperInventoryRenderer renderer;

  private CraftuxUiHost(
      CraftuxActionBus actions,
      BukkitInventoryPort port,
      InventoryRuntime inventory,
      PaperInventoryRenderer renderer) {
    this.actions = actions;
    this.port = port;
    this.inventory = inventory;
    this.renderer = renderer;
  }

  /** Builds the host and registers the Paper inventory listener. */
  public static CraftuxUiHost create(Plugin plugin) {
    CraftuxActionBus actions = new CraftuxActionBus(ALL_ACTIONS);
    BukkitInventoryPort port = new BukkitInventoryPort();
    InventoryRuntime inventory = new InventoryRuntime(port, actions.proxies());
    PaperInventoryRenderer renderer = new PaperInventoryRenderer(plugin, port, inventory);
    return new CraftuxUiHost(actions, port, inventory, renderer);
  }

  /** Actions. */
  public CraftuxActionBus actions() {
    return actions;
  }

  /** Inventory. */
  public InventoryRuntime inventory() {
    return inventory;
  }

  /** Port. */
  public BukkitInventoryPort port() {
    return port;
  }

  /** Renderer. */
  public PaperInventoryRenderer renderer() {
    return renderer;
  }

  /** Closes every open craftux inventory session (plugin disable). */
  public void closeAll() {
    inventory.closeAll();
  }
}
