package net.aincraft.gui;

import com.google.inject.Inject;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.hooks.JobPetsHook;
import net.aincraft.service.JobService;
import net.aincraft.service.PetUpgradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Pet selection GUI using TriumphGUI.
 */
public final class PetSelectionGui {

  private static final int GUI_ROWS = 3;
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  private final PetUpgradeService petUpgradeService;
  private final JobPetsHook jobPetsHook;
  private final JobService jobService;

  // Pet display configuration
  private record PetDisplay(Material icon, String displayName, String description, List<String> perks) {}

  // Pet config name -> display properties
  private static final Map<String, PetDisplay> PET_DISPLAYS = Map.ofEntries(
      Map.entry("allay", new PetDisplay(
          Material.ALLAY_SPAWN_EGG,
          "Allay",
          "A helpful spirit that aids mining",
          List.of("Pet Light", "Ore Search", "Flower Porting", "Azalea Flowering")
      )),
      Map.entry("bat", new PetDisplay(
          Material.BAT_SPAWN_EGG,
          "Bat",
          "A cave dweller with enhanced senses",
          List.of("Silent Walk", "Dripstone Immunity", "Echolocation", "Tiny Wings")
      )),
      Map.entry("goat", new PetDisplay(
          Material.GOAT_SPAWN_EGG,
          "Goat",
          "A sturdy mountain climber",
          List.of("Sturdy Legs", "Goat Mount", "Sticky Blast", "Commander's Blowhorn")
      )),
      Map.entry("copper_golem", new PetDisplay(
          Material.COPPER_BLOCK,
          "Copper Golem",
          "An explosive companion",
          List.of("Blast Radius", "Gunpowder Drop")
      )),
      Map.entry("silverfish", new PetDisplay(
          Material.SILVERFISH_SPAWN_EGG,
          "Silverfish",
          "A default mining companion",
          List.of()
      ))
  );

  // Default display for unknown pets
  private static final PetDisplay DEFAULT_DISPLAY = new PetDisplay(
      Material.BARRIER,
      null,
      "A mysterious companion",
      List.of()
  );

  @Inject
  public PetSelectionGui(PetUpgradeService petUpgradeService,
                         JobPetsHook jobPetsHook, JobService jobService) {
    this.petUpgradeService = petUpgradeService;
    this.jobPetsHook = jobPetsHook;
    this.jobService = jobService;
  }

  public void open(Player player, Job job) {
    List<String> availablePets = petUpgradeService.getAvailablePets(job.key().toString());

    // Handle 0 pets gracefully
    if (availablePets.isEmpty()) {
      player.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.GRAY))
          .append(Component.text("Jobs", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.GRAY))
          .append(Component.text("No pets available for this job!", NamedTextColor.RED))
          .build());
      return;
    }

    // Create dynamic title with job name
    Gui gui = Gui.gui()
        .title(Component.text("Choose Your Pet - ", NamedTextColor.DARK_GREEN)
            .append(job.displayName()))
        .rows(GUI_ROWS)
        .create();

    // Cancel all clicks by default
    gui.setDefaultClickAction(event -> event.setCancelled(true));

    String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), job.key().toString());

    // Fill borders with gray stained glass panes
    GuiItem borderPane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
        .setName(" ")
        .asGuiItem();

    // Top row (0-8)
    for (int i = 0; i < 9; i++) {
      gui.setItem(i, borderPane);
    }
    // Bottom row (18-26)
    for (int i = 18; i < 27; i++) {
      gui.setItem(i, borderPane);
    }
    // Left column (9, 18 already set)
    gui.setItem(9, borderPane);
    // Right column (17, 26 already set)
    gui.setItem(17, borderPane);

    // Calculate centered slots for pets in middle row
    int[] slots = calculateCenteredSlots(availablePets.size());

    // Place pets in calculated slots
    for (int i = 0; i < availablePets.size(); i++) {
      String petName = availablePets.get(i);
      GuiItem item = createPetItem(player, job, petName, petName.equals(selectedPet));
      gui.setItem(slots[i], item);
    }

    gui.open(player);
  }

  /**
   * Calculate centered slot positions in middle row based on pet count.
   * Middle row slots are 9-17, with slot 13 as center.
   */
  private int[] calculateCenteredSlots(int petCount) {
    return switch (petCount) {
      case 1 -> new int[]{13};
      case 2 -> new int[]{12, 14};
      case 3 -> new int[]{11, 13, 15};
      case 4 -> new int[]{10, 12, 14, 16};
      case 5 -> new int[]{11, 12, 13, 14, 15};
      case 6 -> new int[]{10, 11, 12, 14, 15, 16};
      case 7 -> new int[]{10, 11, 12, 13, 14, 15, 16};
      case 8 -> new int[]{9, 10, 11, 12, 14, 15, 16, 17};
      default -> new int[]{13};
    };
  }

  private GuiItem createPetItem(Player player, Job job, String petConfigName, boolean isSelected) {
    // Get display configuration, fall back to default if not found
    PetDisplay display = PET_DISPLAYS.getOrDefault(petConfigName.toLowerCase(), DEFAULT_DISPLAY);

    // Determine display name
    String displayName = display.displayName != null ? display.displayName : formatPetName(petConfigName);

    // Set title with selection indicator - convert to legacy string
    NamedTextColor nameColor = isSelected ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
    String fullDisplayName = isSelected ? displayName + " (Selected)" : displayName;
    String nameString = LEGACY.serialize(
        Component.text(fullDisplayName, nameColor).decoration(TextDecoration.ITALIC, false)
    );

    ItemBuilder builder = ItemBuilder.from(display.icon).setName(nameString);

    // Build lore - convert Adventure Components to legacy strings
    List<String> lore = new ArrayList<>();

    // Add description
    lore.add(LEGACY.serialize(
        Component.text(display.description, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
    ));

    // Add perks if available
    if (!display.perks.isEmpty()) {
      lore.add("");
      lore.add(LEGACY.serialize(
          Component.text("Perks:", NamedTextColor.GOLD)
              .decoration(TextDecoration.ITALIC, false)
      ));
      for (String perk : display.perks) {
        lore.add(LEGACY.serialize(
            Component.text("  • " + perk, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
        ));
      }
    }

    lore.add("");

    // Add click prompt
    if (isSelected) {
      lore.add(LEGACY.serialize(
          Component.text("Currently selected!", NamedTextColor.GREEN)
              .decoration(TextDecoration.ITALIC, false)
      ));
    } else {
      lore.add(LEGACY.serialize(
          Component.text("Click to select this pet", NamedTextColor.AQUA)
              .decoration(TextDecoration.ITALIC, false)
      ));
    }

    builder.setLore(lore);

    // Create click handler
    return builder.asGuiItem(event -> {
      event.setCancelled(true);

      if (isSelected) {
        // Already selected, do nothing
        return;
      }

      String jobKey = job.key().toString();

      // Get current selected pet BEFORE setting new one
      String oldPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);

      // Get player's current level for this job
      int currentLevel = getPlayerLevel(player, jobKey);

      // Revoke old pet perks if switching
      if (oldPet != null && !oldPet.equals(petConfigName)) {
        jobPetsHook.revokePetPermission(player, oldPet);
        revokeOldPetPerks(player, job, oldPet);
      }

      // Set new pet selection
      petUpgradeService.setSelectedPet(player.getUniqueId(), jobKey, petConfigName);

      // Sync pet selection to jobpets-core
      jobPetsHook.syncPetTypeToJobPets(player, petConfigName,
          org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(PetSelectionGui.class));

      // Grant new pet permission
      jobPetsHook.grantPetPermission(player, petConfigName);

      // Grant new pet perks up to current level
      grantNewPetPerks(player, job, petConfigName);

      // Apply revokes for new pet (revoke universal perks that conflict)
      applyPetRevokes(player, job, petConfigName);

      // Restore universal perks that old pet revoked but new pet doesn't
      if (oldPet != null && !oldPet.equals(petConfigName)) {
        restoreRevokedPerks(player, job, oldPet, petConfigName, currentLevel);
      }

      // Notify and close
      player.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.GRAY))
          .append(Component.text("Jobs", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.GRAY))
          .append(Component.text("You selected ", NamedTextColor.GREEN))
          .append(Component.text(formatPetName(petConfigName), NamedTextColor.YELLOW))
          .append(Component.text(" as your job pet!", NamedTextColor.GREEN))
          .build());

      player.closeInventory();
    });
  }

  private String formatPetName(String configName) {
    String[] parts = configName.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (!result.isEmpty()) {
        result.append(" ");
      }
      result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
    }
    return result.toString();
  }

  private void revokeOldPetPerks(Player player, Job job, String oldPet) {
    Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
    Map<Integer, List<String>> oldPetPerks = petPerks.get(oldPet);
    if (oldPetPerks != null) {
      for (List<String> perks : oldPetPerks.values()) {
        for (String perk : perks) {
          jobPetsHook.revokePerkPermission(player, perk);
        }
      }
    }
  }

  private void grantNewPetPerks(Player player, Job job, String newPet) {
    // Get player's current level for this job
    int currentLevel = getPlayerLevel(player, job.key().toString());
    if (currentLevel < 1) {
      return;
    }

    Map<String, Map<Integer, List<String>>> petPerks = job.petPerks();
    Map<Integer, List<String>> newPetPerks = petPerks.get(newPet);
    if (newPetPerks != null) {
      for (Map.Entry<Integer, List<String>> entry : newPetPerks.entrySet()) {
        if (entry.getKey() <= currentLevel) {
          for (String perk : entry.getValue()) {
            jobPetsHook.grantPerkPermission(player, perk);
          }
        }
      }
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
