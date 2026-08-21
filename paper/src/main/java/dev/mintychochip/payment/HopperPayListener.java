package dev.mintychochip.payment;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

/** Marks hopper-filled containers so smelt and brew rewards can be skipped. */
final class HopperPayListener implements Listener {

  private final HopperPayDisableStore store;
  private final ExploitService exploitService;

  HopperPayListener(HopperPayDisableStore store, ExploitService exploitService) {
    this.store = store;
    this.exploitService = exploitService;
  }

  HopperPayDisableStore store() {
    return store;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onMoveToFurnace(final InventoryMoveItemEvent event) {
    if (!exploitService.settings().preventHopperSmelt()) {
      return;
    }
    if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
      return;
    }
    InventoryHolder holder = event.getDestination().getHolder();
    if (holder instanceof Furnace furnace) {
      store.disable(furnace.getBlock());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onMoveToBrewing(final InventoryMoveItemEvent event) {
    if (!exploitService.settings().preventHopperBrew()) {
      return;
    }
    if (event.getDestination().getType() != InventoryType.BREWING) {
      return;
    }
    if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
      return;
    }
    InventoryHolder holder = event.getDestination().getHolder();
    if (holder instanceof BrewingStand stand) {
      store.disable(stand.getBlock());
    }
  }

  /** Opening a machine re-enables payment attribution. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onInteract(final PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    Material type = block.getType();
    if (type == Material.FURNACE
        || type == Material.BLAST_FURNACE
        || type == Material.SMOKER
        || type == Material.BREWING_STAND) {
      store.clear(block);
    }
  }
}
