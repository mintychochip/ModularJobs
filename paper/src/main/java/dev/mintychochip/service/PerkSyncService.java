package dev.mintychochip.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dev.mintychochip.Job;
import dev.mintychochip.hooks.PermissionHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Admin perk grant/revoke without player notifications.
 *
 */
public final class PerkSyncService {

  private final PermissionHook permissions;

  public PerkSyncService(PermissionHook permissions) {
    this.permissions = permissions;
  }

  public void syncPerksToLevel(@NotNull Player player, @NotNull Job job, int targetLevel) {
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    String highestStoragePerk = null;
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() <= targetLevel) {
        for (String perk : entry.getValue()) {
          if (perk.startsWith("storage.")) {
            highestStoragePerk = perk;
          } else {
            permissions.grantPerkPermission(player, perk);
          }
        }
      }
    }
    if (highestStoragePerk != null) {
      for (String storagePerm : getAllStoragePermissions(job)) {
        if (!storagePerm.equals(highestStoragePerk)) {
          permissions.revokePerkPermission(player, storagePerm);
        }
      }
      permissions.grantPerkPermission(player, highestStoragePerk);
    }
  }

  public void revokePerksAboveLevel(@NotNull Player player, @NotNull Job job, int targetLevel) {
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() > targetLevel) {
        for (String perk : entry.getValue()) {
          permissions.revokePerkPermission(player, perk);
        }
      }
    }
    syncPerksToLevel(player, job, targetLevel);
  }

  private List<String> getAllStoragePermissions(Job job) {
    List<String> storage = new ArrayList<>();
    for (List<String> perks : job.perkUnlocks().values()) {
      for (String perk : perks) {
        if (perk.startsWith("storage.")) {
          storage.add(perk);
        }
      }
    }
    return storage;
  }
}
