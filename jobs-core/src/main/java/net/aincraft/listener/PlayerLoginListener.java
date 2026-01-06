package net.aincraft.listener;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.hooks.JobPetsHook;
import net.aincraft.service.JobService;
import net.aincraft.service.PetUpgradeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerLoginListener implements Listener {

  private final JobService jobService;
  private final PetUpgradeService petUpgradeService;
  private final JobPetsHook jobPetsHook;

  @Inject
  public PlayerLoginListener(
      JobService jobService,
      PetUpgradeService petUpgradeService,
      JobPetsHook jobPetsHook) {
    this.jobService = jobService;
    this.petUpgradeService = petUpgradeService;
    this.jobPetsHook = jobPetsHook;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<JobProgression> progressions = jobService.getProgressions(player);

    // Restore permissions for each job the player is in
    for (JobProgression progression : progressions) {
      Job job = progression.job();
      int level = progression.level();
      String jobKey = job.key().toString();

      // Grant jobpets.pet permission for miner job
      if ("modularjobs:miner".equals(jobKey)) {
        jobPetsHook.grantPermission(player, "jobpets.pet");
      }

      // Check if player has a saved pet selection (need this first for revokes)
      String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);
      List<String> revokes = selectedPet != null ?
          job.petRevokedPerks().getOrDefault(selectedPet, List.of()) : List.of();

      // Grant all universal perks up to current level (except revoked ones)
      // Process storage permissions specially to only grant the highest one
      Map<Integer, List<String>> unlocks = job.perkUnlocks();
      String highestStoragePerk = null;
      for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
        if (entry.getKey() <= level) {
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
        jobPetsHook.grantPerkPermission(player, highestStoragePerk);
      }

      // Restore pet if player has a saved selection
      if (selectedPet != null) {
        // Sync pet selection to jobpets-core
        jobPetsHook.syncPetTypeToJobPets(player, selectedPet, null);

        // Restore their pet permission
        jobPetsHook.grantPetPermission(player, selectedPet);

        // Grant all pet-specific perks up to current level
        // Process storage permissions specially to only grant the highest one
        Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
        Map<Integer, List<String>> petLevelPerks = petPerks.get(selectedPet);
        if (petLevelPerks != null) {
          String highestPetStoragePerk = null;
          for (Map.Entry<Integer, List<String>> entry : petLevelPerks.entrySet()) {
            if (entry.getKey() <= level) {
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
            jobPetsHook.grantPerkPermission(player, highestPetStoragePerk);
          }
        }
      }
    }
  }
}
