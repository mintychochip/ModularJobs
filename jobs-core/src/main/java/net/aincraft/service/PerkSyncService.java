package net.aincraft.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.hooks.JobPetsHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Service to handle perk granting/revocation for admin-level commands.
 * Syncs perks without sending notifications to players.
 */
public final class PerkSyncService {

  private final JobPetsHook jobPetsHook;
  private final PetUpgradeService petUpgradeService;

  @Inject
  public PerkSyncService(JobPetsHook jobPetsHook, PetUpgradeService petUpgradeService) {
    this.jobPetsHook = jobPetsHook;
    this.petUpgradeService = petUpgradeService;
  }

  /**
   * Grants all perks from level 1 up to targetLevel.
   * Used when admin adds levels to a player.
   *
   * @param player The player to grant perks to
   * @param job The job
   * @param targetLevel The target level to sync perks to
   */
  public void syncPerksToLevel(@NotNull Player player, @NotNull Job job, int targetLevel) {
    String jobKey = job.key().toString();
    String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);
    List<String> revokes = selectedPet != null ?
        job.petRevokedPerks().getOrDefault(selectedPet, List.of()) : List.of();

    // Grant all universal perks up to target level (except revoked ones)
    // Process storage permissions specially to only grant the highest one
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    String highestStoragePerk = null;
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() <= targetLevel) {
        for (String perk : entry.getValue()) {
          if (perk.startsWith("storage.")) {
            highestStoragePerk = perk; // Will be overwritten by higher levels
          } else if (!revokes.contains(perk)) {
            jobPetsHook.grantPerkPermission(player, perk);
          }
        }
      }
    }
    if (highestStoragePerk != null && !revokes.contains(highestStoragePerk)) {
      // Revoke all other storage perks first
      for (String storagePerm : getAllStoragePermissions(job)) {
        if (!storagePerm.equals(highestStoragePerk)) {
          jobPetsHook.revokePerkPermission(player, storagePerm);
        }
      }
      jobPetsHook.grantPerkPermission(player, highestStoragePerk);
    }

    // Grant all pet-specific perks up to target level if player has a selected pet
    if (selectedPet != null) {
      Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
      Map<Integer, List<String>> petLevelPerks = petPerks.get(selectedPet);
      if (petLevelPerks != null) {
        String highestPetStoragePerk = null;
        for (Map.Entry<Integer, List<String>> entry : petLevelPerks.entrySet()) {
          if (entry.getKey() <= targetLevel) {
            for (String perk : entry.getValue()) {
              if (perk.startsWith("storage.")) {
                highestPetStoragePerk = perk;
              } else {
                jobPetsHook.grantPerkPermission(player, perk);
              }
            }
          }
        }
        if (highestPetStoragePerk != null) {
          // Revoke the universal storage perk if pet has a storage perk
          if (highestStoragePerk != null) {
            jobPetsHook.revokePerkPermission(player, highestStoragePerk);
          }
          // Revoke all other storage perks first
          for (String storagePerm : getAllStoragePermissions(job)) {
            if (!storagePerm.equals(highestPetStoragePerk)) {
              jobPetsHook.revokePerkPermission(player, storagePerm);
            }
          }
          jobPetsHook.grantPerkPermission(player, highestPetStoragePerk);
        }
      }
    }
  }

  /**
   * Revokes perks that are above targetLevel.
   * Used when admin subtracts levels from a player.
   *
   * @param player The player to revoke perks from
   * @param job The job
   * @param targetLevel The target level - perks above this will be revoked
   */
  public void revokePerksAboveLevel(@NotNull Player player, @NotNull Job job, int targetLevel) {
    String jobKey = job.key().toString();
    String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);

    // Revoke universal perks above target level
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() > targetLevel) {
        for (String perk : entry.getValue()) {
          jobPetsHook.revokePerkPermission(player, perk);
        }
      }
    }

    // Revoke pet-specific perks above target level if player has a selected pet
    if (selectedPet != null) {
      Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
      Map<Integer, List<String>> petLevelPerks = petPerks.get(selectedPet);
      if (petLevelPerks != null) {
        for (Map.Entry<Integer, List<String>> entry : petLevelPerks.entrySet()) {
          if (entry.getKey() > targetLevel) {
            for (String perk : entry.getValue()) {
              jobPetsHook.revokePerkPermission(player, perk);
            }
          }
        }
      }
    }

    // Re-sync remaining perks to ensure proper storage permission handling
    syncPerksToLevel(player, job, targetLevel);
  }

  private List<String> getAllStoragePermissions(Job job) {
    List<String> storage = new ArrayList<>();
    // From universal perks
    for (List<String> perks : job.perkUnlocks().values()) {
      for (String perk : perks) {
        if (perk.startsWith("storage.")) {
          storage.add(perk);
        }
      }
    }
    // From pet perks
    for (Map<Integer, List<String>> petPerks : job.petPerks().values()) {
      for (List<String> perks : petPerks.values()) {
        for (String perk : perks) {
          if (perk.startsWith("storage.")) {
            storage.add(perk);
          }
        }
      }
    }
    return storage;
  }
}
