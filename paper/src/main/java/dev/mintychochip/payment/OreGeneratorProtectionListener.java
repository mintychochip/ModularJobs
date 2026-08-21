package dev.mintychochip.payment;

import dev.mintychochip.payment.ExploitService.ExploitProtectionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

/**
 * Generator-formed stone, cobblestone, and obsidian follow the configured protection policy.
 * When {@link ExploitProtectionSettings#protectOreGenerators()} is true, stone/cobble/obsidian
 * formed by generators keep PLACED protection (no quick re-break pay). When false, protection is
 * cleared so generator breaks can pay.
 */
final class OreGeneratorProtectionListener implements Listener {

  private final ExploitService exploitService;

  OreGeneratorProtectionListener(ExploitService exploitService) {
    this.exploitService = exploitService;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onBlockFromTo(final BlockFromToEvent event) {
    Block to = event.getToBlock();
    Material mat = to.getType();
    if (mat.isAir()) {
      return;
    }
    if (mat == Material.WATER || mat == Material.LAVA
        || event.getBlock().getType() == Material.WATER
        || event.getBlock().getType() == Material.LAVA) {
      // liquid flow itself is not a solid generator result we care about when destination is liquid
      if (mat == Material.WATER || mat == Material.LAVA) {
        return;
      }
    }
    boolean generatorMaterial =
        mat == Material.STONE || mat == Material.COBBLESTONE || mat == Material.OBSIDIAN;
    if (exploitService.settings().protectOreGenerators() && generatorMaterial) {
      // Keep any existing protection; if none, mark so first generator cycle cannot be farmed
      if (exploitService.canProtect(ExploitProtectionType.PLACED, to)
          && !exploitService.isProtected(ExploitProtectionType.PLACED, to)) {
        exploitService.addProtection(ExploitProtectionType.PLACED, to);
      }
      return;
    }
    // When not protecting generators, clear so natural/generator breaks can pay
    if (generatorMaterial && exploitService.canProtect(ExploitProtectionType.PLACED, to)) {
      exploitService.removeProtection(ExploitProtectionType.PLACED, to);
    }
  }
}
