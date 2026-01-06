package net.aincraft.listener;

// DISABLED: JobPets dependency not available
/*
import com.aincraft.jobpets.api.event.PetChangeEvent;
import com.google.inject.Inject;
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

public final class PetChangeListener implements Listener {

  private final JobService jobService;
  private final PetUpgradeService petUpgradeService;
  private final JobPetsHook jobPetsHook;

  @Inject
  public PetChangeListener(JobService jobService, PetUpgradeService petUpgradeService,
                           JobPetsHook jobPetsHook) {
    this.jobService = jobService;
    this.petUpgradeService = petUpgradeService;
    this.jobPetsHook = jobPetsHook;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPetChange(PetChangeEvent event) {
    Player player = event.getPlayer();
    String jobKey = event.getJobKey();
    String oldPet = event.getOldPetConfigName();
    String newPet = event.getNewPetConfigName();

    // Get job from job key
    Job job;
    try {
      job = jobService.getJob(jobKey);
    } catch (IllegalArgumentException e) {
      return; // Not a ModularJobs job
    }

    // Get player's current level
    int currentLevel = getPlayerLevel(player, jobKey);
    if (currentLevel < 1) {
      return; // Player not in this job
    }

    // Update PetUpgradeService with new selection
    petUpgradeService.setSelectedPet(player.getUniqueId(), jobKey, newPet);

    // Revoke old pet perks
    if (oldPet != null) {
      Map<Integer, List<String>> oldPetPerks = job.petPerks().get(oldPet);
      if (oldPetPerks != null) {
        for (List<String> perks : oldPetPerks.values()) {
          for (String perk : perks) {
            jobPetsHook.revokePerkPermission(player, perk);
          }
        }
      }
    }

    // Grant new pet perks up to current level
    Map<Integer, List<String>> newPetPerks = job.petPerks().get(newPet);
    if (newPetPerks != null) {
      for (Map.Entry<Integer, List<String>> entry : newPetPerks.entrySet()) {
        if (entry.getKey() <= currentLevel) {
          for (String perk : entry.getValue()) {
            jobPetsHook.grantPerkPermission(player, perk);
          }
        }
      }
    }

    // Apply revokes for new pet (revoke universal perks that conflict)
    applyPetRevokes(player, job, newPet);

    // Restore universal perks that old pet revoked but new pet doesn't
    if (oldPet != null) {
      restoreRevokedPerks(player, job, oldPet, newPet, currentLevel);
    }
  }

  private int getPlayerLevel(Player player, String jobKey) {
    List<JobProgression> progressions = jobService.getProgressions(player);
    for (JobProgression prog : progressions) {
      if (prog.job().key().toString().equals(jobKey)) {
        return prog.level();
      }
    }
    return 0;
  }

  private void applyPetRevokes(Player player, Job job, String petConfigName) {
    List<String> revokes = job.petRevokedPerks().get(petConfigName);
    if (revokes != null) {
      for (String perk : revokes) {
        jobPetsHook.revokePerkPermission(player, perk);
      }
    }
  }

  private void restoreRevokedPerks(Player player, Job job, String oldPet, String newPet, int level) {
    // Get perks that old pet revoked
    List<String> oldRevokes = job.petRevokedPerks().get(oldPet);
    if (oldRevokes == null) {
      return;
    }

    // Get perks that new pet revokes
    List<String> newRevokes = job.petRevokedPerks().getOrDefault(newPet, List.of());

    // Restore perks that were revoked by old pet but NOT by new pet
    Map<Integer, List<String>> universalPerks = job.perkUnlocks();
    for (Map.Entry<Integer, List<String>> entry : universalPerks.entrySet()) {
      if (entry.getKey() <= level) {
        for (String perk : entry.getValue()) {
          if (oldRevokes.contains(perk) && !newRevokes.contains(perk)) {
            jobPetsHook.grantPerkPermission(player, perk);
          }
        }
      }
    }
  }
}
*/