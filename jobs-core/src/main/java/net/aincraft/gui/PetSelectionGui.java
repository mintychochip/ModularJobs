package net.aincraft.gui;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.hooks.JobPetsHook;
import net.aincraft.service.JobService;
import net.aincraft.service.PetUpgradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class PetSelectionGui implements Listener {

  private static final int GUI_SIZE = 27; // 3 rows

  private final Plugin plugin;
  private final PetUpgradeService petUpgradeService;
  private final JobPetsHook jobPetsHook;
  private final JobService jobService;
  private final Map<UUID, Job> openGuis = new HashMap<>();
  private final NamespacedKey petConfigKey;

  // Pet display configuration
  private static class PetDisplay {
    final Material icon;
    final String displayName;
    final String description;
    final List<String> perks;

    PetDisplay(Material icon, String displayName, String description, List<String> perks) {
      this.icon = icon;
      this.displayName = displayName;
      this.description = description;
      this.perks = perks;
    }
  }

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
      null, // Will use formatted config name
      "A mysterious companion",
      List.of()
  );

  @Inject
  public PetSelectionGui(Plugin plugin, PetUpgradeService petUpgradeService,
                         JobPetsHook jobPetsHook, JobService jobService) {
    this.plugin = plugin;
    this.petUpgradeService = petUpgradeService;
    this.jobPetsHook = jobPetsHook;
    this.jobService = jobService;
    this.petConfigKey = new NamespacedKey(plugin, "pet_config");
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
    Component title = Component.text("Choose Your Pet - ", NamedTextColor.DARK_GREEN)
        .append(job.displayName());

    Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);
    String selectedPet = petUpgradeService.getSelectedPet(player.getUniqueId(), job.key().toString());

    // Fill borders with gray stained glass panes
    ItemStack borderPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta paneMeta = borderPane.getItemMeta();
    paneMeta.displayName(Component.text(" ")); // Empty name
    borderPane.setItemMeta(paneMeta);

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
      ItemStack item = createPetItem(petName, petName.equals(selectedPet));
      gui.setItem(slots[i], item);
    }

    openGuis.put(player.getUniqueId(), job);
    player.openInventory(gui);
  }

  /**
   * Calculate centered slot positions in middle row based on pet count.
   * Middle row slots are 9-17, with slot 13 as center.
   *
   * @param petCount number of pets (1-8)
   * @return array of slot indices
   */
  private int[] calculateCenteredSlots(int petCount) {
    return switch (petCount) {
      case 1 -> new int[]{13};                          // Center
      case 2 -> new int[]{12, 14};                      // Around center
      case 3 -> new int[]{11, 13, 15};                  // 3 centered
      case 4 -> new int[]{10, 12, 14, 16};              // 4 centered (skip 13)
      case 5 -> new int[]{11, 12, 13, 14, 15};          // 5 centered
      case 6 -> new int[]{10, 11, 12, 14, 15, 16};      // 6 centered (skip 13)
      case 7 -> new int[]{10, 11, 12, 13, 14, 15, 16};  // 7 centered
      case 8 -> new int[]{9, 10, 11, 12, 14, 15, 16, 17}; // Full row (skip 13)
      default -> new int[]{13}; // Fallback to center
    };
  }

  private ItemStack createPetItem(String petConfigName, boolean isSelected) {
    // Get display configuration, fall back to default if not found
    PetDisplay display = PET_DISPLAYS.getOrDefault(petConfigName.toLowerCase(), DEFAULT_DISPLAY);

    // Create item with appropriate material
    ItemStack item = new ItemStack(display.icon);
    ItemMeta meta = item.getItemMeta();

    // Determine display name
    String displayName = display.displayName != null ? display.displayName : formatPetName(petConfigName);

    // Set title with selection indicator
    if (isSelected) {
      meta.displayName(Component.text(displayName + " (Selected)", NamedTextColor.GREEN)
          .decoration(TextDecoration.ITALIC, false));
    } else {
      meta.displayName(Component.text(displayName, NamedTextColor.YELLOW)
          .decoration(TextDecoration.ITALIC, false));
    }

    List<Component> lore = new ArrayList<>();

    // Add description
    lore.add(Component.text(display.description, NamedTextColor.GRAY)
        .decoration(TextDecoration.ITALIC, false));

    // Add perks if available
    if (!display.perks.isEmpty()) {
      lore.add(Component.empty()); // Blank line
      lore.add(Component.text("Perks:", NamedTextColor.GOLD)
          .decoration(TextDecoration.ITALIC, false));
      for (String perk : display.perks) {
        lore.add(Component.text("  \u2022 " + perk, NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
      }
    }

    lore.add(Component.empty()); // Blank line separator

    // Add click prompt
    if (isSelected) {
      lore.add(Component.text("Currently selected!", NamedTextColor.GREEN)
          .decoration(TextDecoration.ITALIC, false));
    } else {
      lore.add(Component.text("Click to select this pet", NamedTextColor.AQUA)
          .decoration(TextDecoration.ITALIC, false));
    }

    meta.lore(lore);

    // Store pet name in persistent data
    meta.getPersistentDataContainer().set(petConfigKey, PersistentDataType.STRING, petConfigName);

    item.setItemMeta(meta);
    return item;
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

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (!openGuis.containsKey(player.getUniqueId())) {
      return;
    }

    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) {
      return;
    }

    ItemMeta meta = clicked.getItemMeta();
    if (meta == null) {
      return;
    }

    String petConfig = meta.getPersistentDataContainer().get(petConfigKey, PersistentDataType.STRING);

    if (petConfig == null) {
      return;
    }

    Job job = openGuis.get(player.getUniqueId());
    String jobKey = job.key().toString();

    // Get current selected pet BEFORE setting new one
    String oldPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey);

    // Get player's current level for this job
    int currentLevel = getPlayerLevel(player, jobKey);

    // Revoke old pet perks if switching
    if (oldPet != null && !oldPet.equals(petConfig)) {
      jobPetsHook.revokePetPermission(player, oldPet);
      revokeOldPetPerks(player, job, oldPet);
    }

    // Set new pet selection
    petUpgradeService.setSelectedPet(player.getUniqueId(), jobKey, petConfig);

    // Sync pet selection to jobpets-core
    jobPetsHook.syncPetTypeToJobPets(player, petConfig, plugin);

    // Grant new pet permission
    jobPetsHook.grantPetPermission(player, petConfig);

    // Grant new pet perks up to current level
    grantNewPetPerks(player, job, petConfig);

    // Apply revokes for new pet (revoke universal perks that conflict)
    applyPetRevokes(player, job, petConfig);

    // Restore universal perks that old pet revoked but new pet doesn't
    if (oldPet != null && !oldPet.equals(petConfig)) {
      restoreRevokedPerks(player, job, oldPet, petConfig, currentLevel);
    }

    // Notify and close
    player.sendMessage(Component.text()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("Jobs", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.GRAY))
        .append(Component.text("You selected ", NamedTextColor.GREEN))
        .append(Component.text(formatPetName(petConfig), NamedTextColor.YELLOW))
        .append(Component.text(" as your job pet!", NamedTextColor.GREEN))
        .build());

    player.closeInventory();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player) {
      openGuis.remove(player.getUniqueId());
    }
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
      return; // Player not in this job
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
