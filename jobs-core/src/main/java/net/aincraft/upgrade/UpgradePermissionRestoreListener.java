package net.aincraft.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Restores upgrade permissions on login and cleans up on logout.
 */
@Singleton
public final class UpgradePermissionRestoreListener implements Listener {
  private final UpgradeService upgradeService;
  private final UpgradeEffectApplier effectApplier;
  private final UpgradePermissionManager permissionManager;

  @Inject
  public UpgradePermissionRestoreListener(
      UpgradeService upgradeService,
      UpgradeEffectApplier effectApplier,
      UpgradePermissionManager permissionManager
  ) {
    this.upgradeService = upgradeService;
    this.effectApplier = effectApplier;
    this.permissionManager = permissionManager;
  }

  /**
   * Restore all upgrade permissions from all job trees when player joins.
   * Includes both base effects and level-specific effects for upgraded nodes.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    String playerId = player.getUniqueId().toString();

    // Get all trees and restore effects from unlocked nodes
    upgradeService.getAllTrees().forEach(tree -> {
      PlayerUpgradeData data = upgradeService.getPlayerData(playerId, tree.jobKey());
      if (!data.unlockedNodes().isEmpty()) {
        effectApplier.restoreEffects(player, tree, data);
      }
    });
  }

  /**
   * Cleanup all permission attachments when player quits.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    permissionManager.cleanupPlayer(event.getPlayer().getUniqueId());
  }
}
