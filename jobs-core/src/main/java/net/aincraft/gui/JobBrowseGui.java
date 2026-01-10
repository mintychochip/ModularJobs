package net.aincraft.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.mintychochip.mint.Mint;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * GUI for browsing and joining available jobs using TriumphGUI.
 */
@Singleton
public final class JobBrowseGui {

  private static final int GUI_ROWS = 6;
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

  private final JobService jobService;
  private final UpgradeService upgradeService;

  @Inject
  public JobBrowseGui(JobService jobService, UpgradeService upgradeService) {
    this.jobService = jobService;
    this.upgradeService = upgradeService;
  }

  /**
   * Open the job browse GUI for a player.
   */
  public void open(Player player) {
    Gui gui = Gui.gui()
        .title(Component.text("Browse Jobs", NamedTextColor.AQUA))
        .rows(GUI_ROWS)
        .create();

    // Cancel all clicks by default
    gui.setDefaultClickAction(event -> event.setCancelled(true));

    // Fill background
    fillBackground(gui);

    // Get all jobs and player's current jobs
    List<Job> allJobs = jobService.getJobs();
    List<JobProgression> playerJobs = jobService.getProgressions(player);

    // Build player job map for quick lookup
    java.util.Map<String, JobProgression> playerJobMap = new java.util.HashMap<>();
    for (JobProgression prog : playerJobs) {
      playerJobMap.put(prog.job().key().asString(), prog);
    }

    // Render job items (rows 1-4, columns 1-7)
    int slot = 10; // Start at row 1, column 1
    for (Job job : allJobs) {
      // Calculate row and column
      int row = slot / 9;
      int col = slot % 9;

      // Ensure we're within valid area (rows 1-4, cols 1-7)
      if (row >= 5) {
        break;
      }

      // If at right edge (col 8), wrap to next row col 1
      if (col == 8) {
        row++;
        col = 1;
        slot = row * 9 + col;
      }

      // If at left edge (col 0), move to col 1
      if (col == 0) {
        col = 1;
        slot = row * 9 + col;
      }

      // Double-check we're still in valid range
      if (row >= 5 || slot >= GUI_ROWS * 9) {
        break;
      }

      JobProgression progression = playerJobMap.get(job.key().asString());
      GuiItem item = createJobItem(player, job, progression);
      gui.setItem(slot, item);
      slot++;
    }

    gui.open(player);
  }

  /**
   * Fill the background with glass panes.
   */
  private void fillBackground(Gui gui) {
    GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
        .setName(" ")
        .asGuiItem();

    for (int i = 0; i < GUI_ROWS * 9; i++) {
      gui.setItem(i, pane);
    }
  }

  /**
   * Create a GuiItem representing a job.
   */
  private GuiItem createJobItem(Player player, Job job, JobProgression progression) {
    boolean isJoined = progression != null;

    // Choose material based on job status
    Material material = isJoined ? Material.EMERALD : Material.BOOK;

    // Display name - convert Adventure Component to legacy string
    NamedTextColor nameColor = isJoined ? NamedTextColor.GREEN : NamedTextColor.GOLD;
    String nameString = LEGACY.serialize(
        job.displayName().color(nameColor).decoration(TextDecoration.ITALIC, false)
    );

    ItemBuilder builder = ItemBuilder.from(material).setName(nameString);

    // Build lore - convert Adventure Components to legacy strings
    List<String> lore = new ArrayList<>();

    // Description
    lore.add(LEGACY.serialize(
        job.description().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
    ));
    lore.add("");

    // Max level info
    lore.add(LEGACY.serialize(
        Component.text()
            .append(Component.text("Max Level: ", NamedTextColor.GRAY))
            .append(Component.text(job.maxLevel(), NamedTextColor.YELLOW))
            .decoration(TextDecoration.ITALIC, false)
            .build()
    ));

    // Active player count
    int activeCount = countActivePlayers(job);
    lore.add(LEGACY.serialize(
        Component.text()
            .append(Component.text("Active Players: ", NamedTextColor.GRAY))
            .append(Component.text(activeCount, NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false)
            .build()
    ));

    // Upgrade tree info
    Optional<UpgradeTree> treeOpt = upgradeService.getTree(job.key().value());
    if (treeOpt.isPresent()) {
      UpgradeTree tree = treeOpt.get();
      lore.add(LEGACY.serialize(
          Component.text()
              .append(Component.text("Upgrade Tree: ", NamedTextColor.GRAY))
              .append(Component.text(tree.allNodes().size() + " nodes", NamedTextColor.LIGHT_PURPLE))
              .decoration(TextDecoration.ITALIC, false)
              .build()
      ));
    }

    // Example rewards
    lore.add("");
    lore.add(LEGACY.serialize(
        Component.text("Example Rewards:", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)
    ));
    addExampleRewards(job, lore);

    // If joined, show progress
    if (isJoined) {
      lore.add("");
      lore.add(LEGACY.serialize(
          Component.text()
              .append(Component.text("Your Level: ", NamedTextColor.GRAY))
              .append(Component.text(progression.level(), NamedTextColor.GREEN))
              .decoration(TextDecoration.ITALIC, false)
              .build()
      ));

      lore.add(LEGACY.serialize(
          Component.text()
              .append(Component.text("Experience: ", NamedTextColor.GRAY))
              .append(Component.text(progression.experience().toPlainString(), NamedTextColor.AQUA))
              .decoration(TextDecoration.ITALIC, false)
              .build()
      ));

      lore.add("");
      lore.add(LEGACY.serialize(
          Component.text("✓ Already Joined", NamedTextColor.GREEN)
              .decoration(TextDecoration.ITALIC, false)
      ));
    } else {
      lore.add("");
      lore.add(LEGACY.serialize(
          Component.text("Click to join!", NamedTextColor.YELLOW)
              .decoration(TextDecoration.ITALIC, false)
      ));
    }

    builder.setLore(lore);

    // Create click handler
    GuiItem item = builder.asGuiItem(event -> {
      event.setCancelled(true);

      // Attempt to join the job
      String playerId = player.getUniqueId().toString();
      String jobKey = job.key().asString();

      try {
        if (jobService.joinJob(playerId, jobKey)) {
          Mint.sendThemedMessage(player, "<primary>✓ You joined</primary> <secondary>" + job.getPlainName() + "</secondary> <primary>!</primary>");
          player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

          // Refresh the GUI to show updated status
          player.closeInventory();
          org.bukkit.Bukkit.getScheduler().runTaskLater(
              org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(JobBrowseGui.class),
              () -> open(player),
              1L
          );
        } else {
          Mint.sendThemedMessage(player, "<neutral>You are already in</neutral> <secondary>" + job.getPlainName() + "</secondary>.");
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        }
      } catch (IllegalArgumentException e) {
        Mint.sendThemedMessage(player, "<error>Job not found: " + jobKey);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
    });

    return item;
  }

  /**
   * Count how many online players are actively in this job.
   */
  private int countActivePlayers(Job job) {
    int count = 0;
    String jobKey = job.key().asString();
    for (Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
      List<JobProgression> progressions = jobService.getProgressions(onlinePlayer);
      for (JobProgression prog : progressions) {
        if (prog.job().key().asString().equals(jobKey)) {
          count++;
          break;
        }
      }
    }
    return count;
  }

  /**
   * Add example rewards to the lore.
   */
  private void addExampleRewards(Job job, List<String> lore) {
    java.util.Map<ActionType, List<JobTask>> allTasks = jobService.getAllTasks(job);

    int examplesAdded = 0;
    int maxExamples = 3;

    for (java.util.Map.Entry<ActionType, List<JobTask>> entry : allTasks.entrySet()) {
      if (examplesAdded >= maxExamples) {
        break;
      }

      ActionType actionType = entry.getKey();
      List<JobTask> tasks = entry.getValue();

      if (tasks.isEmpty()) {
        continue;
      }

      // Get first task as example
      JobTask exampleTask = tasks.get(0);
      List<Payable> payables = exampleTask.payables();

      if (!payables.isEmpty()) {
        // Get the first payable
        Payable payable = payables.get(0);
        BigDecimal amount = payable.amount().value();
        String payableTypeName = payable.type().key().value();

        // Format action type (remove namespace if present)
        String actionName = actionType.key().value();
        if (actionName.contains(":")) {
          actionName = actionName.substring(actionName.indexOf(':') + 1);
        }

        // Format payable type
        String formattedPayable = formatPayableType(payableTypeName);

        lore.add(LEGACY.serialize(
            Component.text()
                .append(Component.text("  • ", NamedTextColor.DARK_GRAY))
                .append(Component.text(actionName, NamedTextColor.YELLOW))
                .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                .append(Component.text(amount.toPlainString() + " " + formattedPayable, NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false)
                .build()
        ));

        examplesAdded++;
      }
    }

    if (examplesAdded == 0) {
      lore.add(LEGACY.serialize(
          Component.text("  No rewards configured", NamedTextColor.GRAY)
              .decoration(TextDecoration.ITALIC, false)
      ));
    }
  }

  /**
   * Format payable type name for display.
   */
  private String formatPayableType(String payableType) {
    // Remove namespace if present
    if (payableType.contains(":")) {
      payableType = payableType.substring(payableType.indexOf(':') + 1);
    }

    // Convert to title case
    String[] parts = payableType.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (result.length() > 0) {
        result.append(" ");
      }
      result.append(part.substring(0, 1).toUpperCase());
      if (part.length() > 1) {
        result.append(part.substring(1).toLowerCase());
      }
    }
    return result.toString();
  }
}
