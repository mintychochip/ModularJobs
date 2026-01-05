package net.aincraft.listener;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.event.JobJoinEvent;
import net.aincraft.event.JobLeaveEvent;
import net.aincraft.event.JobLevelEvent;
import net.aincraft.hooks.JobPetsHook;
import net.aincraft.service.PetUpgradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class JobLevelUpListener implements Listener {

  private final PetUpgradeService petUpgradeService;
  private final JobPetsHook jobPetsHook;
  private final Plugin plugin;
  private final net.aincraft.gui.PetSelectionGui petSelectionGui;

  @Inject
  public JobLevelUpListener(
      PetUpgradeService petUpgradeService,
      JobPetsHook jobPetsHook,
      Plugin plugin,
      net.aincraft.gui.PetSelectionGui petSelectionGui) {
    this.petUpgradeService = petUpgradeService;
    this.jobPetsHook = jobPetsHook;
    this.plugin = plugin;
    this.petSelectionGui = petSelectionGui;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLevelUp(JobLevelEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();
    String jobKey = job.key().toString();

    // Get selected pet to check for revokes
    String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);
    List<String> revokes = selectedPet != null ?
        job.petRevokedPerks().getOrDefault(selectedPet, List.of()) : List.of();

    // Process universal perk unlocks for each level gained
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    for (int level = oldLevel + 1; level <= newLevel; level++) {
      List<String> perks = unlocks.get(level);
      if (perks != null) {
        for (String perk : perks) {
          // Only grant if not revoked by current pet
          if (!revokes.contains(perk)) {
            // If this is a storage permission, revoke all others first
            if (perk.startsWith("storage.")) {
              for (String storagePerm : getAllStoragePermissions(job)) {
                if (!storagePerm.equals(perk)) {
                  jobPetsHook.revokePerkPermission(player, storagePerm);
                }
              }
            }
            jobPetsHook.grantPerkPermission(player, perk);
            notifyPerkUnlock(player, perk, job);
          }
        }
      }
    }

    // Process pet-specific perks if player has selected a pet
    if (selectedPet != null) {
      Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
      Map<Integer, List<String>> petLevelPerks = petPerks.get(selectedPet);
      if (petLevelPerks != null) {
        for (int level = oldLevel + 1; level <= newLevel; level++) {
          List<String> perks = petLevelPerks.get(level);
          if (perks != null) {
            for (String perk : perks) {
              // If this is a storage permission, revoke all others first
              if (perk.startsWith("storage.")) {
                for (String storagePerm : getAllStoragePermissions(job)) {
                  if (!storagePerm.equals(perk)) {
                    jobPetsHook.revokePerkPermission(player, storagePerm);
                  }
                }
              }
              jobPetsHook.grantPerkPermission(player, perk);
              notifyPerkUnlock(player, perk, job);
            }
          }
        }
      }
    }

    // Check if player reached upgrade level for pet selection
    int upgradeLevel = job.upgradeLevel();
    if (newLevel >= upgradeLevel && oldLevel < upgradeLevel) {
      // Only prompt if they haven't already selected a pet
      if (selectedPet == null) {
        player.getServer().getScheduler().runTask(
            plugin,
            () -> openPetSelectionGui(player, job)
        );
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobJoin(JobJoinEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int level = event.getLevel();
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
    // Process storage SIZE permissions specially to only grant the highest one
    // Note: storage.access is the ACCESS permission, not a size - grant it directly
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    String highestStoragePerk = null;
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() <= level) {
        for (String perk : entry.getValue()) {
          if (perk.equals("storage.access")) {
            // storage.access is the base access permission, grant directly
            jobPetsHook.grantPerkPermission(player, perk);
          } else if (perk.startsWith("storage.")) {
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

      if (event.isRejoin()) {
        player.sendMessage(Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("Jobs", NamedTextColor.GOLD))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text("Welcome back! Your ", NamedTextColor.GREEN))
            .append(Component.text(formatPetName(selectedPet), NamedTextColor.YELLOW))
            .append(Component.text(" pet has been restored.", NamedTextColor.GREEN))
            .build());
      }
    } else if (level >= job.upgradeLevel()) {
      // They reached upgrade level but never chose a pet - prompt them
      player.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.GRAY))
          .append(Component.text("Jobs", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.GRAY))
          .append(Component.text("Choose your job pet with ", NamedTextColor.GREEN))
          .append(Component.text("/jobs upgrade " + job.key().value(), NamedTextColor.YELLOW))
          .build());
    }

    if (event.isRejoin()) {
      player.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.GRAY))
          .append(Component.text("Jobs", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.GRAY))
          .append(Component.text("Rejoined at level " + level + ". Perks restored.", NamedTextColor.GREEN))
          .build());
    }
  }

  private List<String> getAllStoragePermissions(Job job) {
    List<String> storage = new ArrayList<>();
    // From universal perks (exclude storage.access - that's the base access permission, not a size)
    for (List<String> perks : job.perkUnlocks().values()) {
      for (String perk : perks) {
        if (perk.startsWith("storage.") && !perk.equals("storage.access")) {
          storage.add(perk);
        }
      }
    }
    // From pet perks
    for (Map<Integer, List<String>> petPerks : job.petPerks().values()) {
      for (List<String> perks : petPerks.values()) {
        for (String perk : perks) {
          if (perk.startsWith("storage.") && !perk.equals("storage.access")) {
            storage.add(perk);
          }
        }
      }
    }
    return storage;
  }

  private void notifyPerkUnlock(Player player, String perkName, Job job) {
    player.sendMessage(Component.text()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("Jobs", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.GRAY))
        .append(Component.text("You unlocked the ", NamedTextColor.GREEN))
        .append(Component.text(perkName, NamedTextColor.YELLOW))
        .append(Component.text(" perk!", NamedTextColor.GREEN))
        .build());
  }

  private void openPetSelectionGui(Player player, Job job) {
    petSelectionGui.open(player, job);
  }

  private String formatPetName(String configName) {
    String[] parts = configName.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (!result.isEmpty()) result.append(" ");
      result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
    }
    return result.toString();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLeave(JobLeaveEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    String jobKey = job.key().toString();

    // Revoke jobpets.pet permission for miner job
    if ("modularjobs:miner".equals(jobKey)) {
      jobPetsHook.revokePermission(player, "jobpets.pet");
    }

    // Revoke all universal perks for this job (includes storage.* perks)
    Map<Integer, List<String>> unlocks = job.perkUnlocks();
    for (List<String> perks : unlocks.values()) {
      for (String perk : perks) {
        jobPetsHook.revokePerkPermission(player, perk);
      }
    }

    // Revoke all pet-specific perks for this job (includes storage.* perks)
    Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
    for (Map<Integer, List<String>> petLevelPerks : petPerks.values()) {
      for (List<String> perks : petLevelPerks.values()) {
        for (String perk : perks) {
          jobPetsHook.revokePerkPermission(player, perk);
        }
      }
    }

    // Revoke all pet permissions for this job's pets
    List<String> jobPets = petUpgradeService.getAvailablePets(jobKey);
    for (String pet : jobPets) {
      jobPetsHook.revokePetPermission(player, pet);
    }

    // Clear pet selection
    // Note: We don't delete from DB so they keep selection if they rejoin

    player.sendMessage(Component.text()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("Jobs", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.GRAY))
        .append(Component.text("You left ", NamedTextColor.RED))
        .append(job.displayName())
        .append(Component.text(". Pet permissions revoked.", NamedTextColor.RED))
        .build());
  }
}
