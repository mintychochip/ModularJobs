package net.aincraft.gui;

import dev.craftux.api.inventory.InventoryClick;
import dev.craftux.api.inventory.InventoryView;
import dev.craftux.api.inventory.ItemSpec;
import dev.craftux.api.inventory.Slot;
import dev.craftux.api.inventory.SlotPixelIntent;
import dev.craftux.common.inventory.InventoryRuntime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.gui.craftux.CraftuxItems;
import net.aincraft.gui.craftux.CraftuxUiHost;
import net.aincraft.service.JobService;
import net.aincraft.service.JoinGate;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Job browse GUI backed by craftux {@link InventoryRuntime}.
 *
 * <p>Views are pure presentation; join side-effects live in the host action
 * registered under {@link CraftuxUiHost#ACTION_JOB_JOIN}.
 */
public final class JobBrowseGui {

  private static final int GUI_ROWS = 6;
  private static final String MENU_ID = "job_browse";
  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  private final InventoryRuntime inventory;
  private final JobService jobService;
  private final UpgradeService upgradeService;
  private final JoinGate joinGate;

  /** Per-audience session: slot index → job key for join dispatch. */
  private final Map<UUID, Map<Integer, String>> sessions = new HashMap<>();

  /** Builds the job-browse presenter over the shared craftux runtime. */
  public JobBrowseGui(
      InventoryRuntime inventory,
      JobService jobService,
      UpgradeService upgradeService,
      JoinGate joinGate) {
    this.inventory = inventory;
    this.jobService = jobService;
    this.upgradeService = upgradeService;
    this.joinGate = joinGate;
  }

  /** Opens the browse menu for {@code player}. */
  public void open(Player player) {
    UUID audience = player.getUniqueId();
    InventoryView view = buildView(player);
    inventory.open(audience, view);
  }

  /**
   * Host action: join the job mapped to the clicked slot.
   * Registered via {@link CraftuxUiHost#actions()}.
   */
  public void onJoin(UUID audience, InventoryClick click) {
    Player player = Bukkit.getPlayer(audience);
    if (player == null) {
      return;
    }
    Map<Integer, String> slotJobs = sessions.get(audience);
    if (slotJobs == null) {
      return;
    }
    String jobKey = slotJobs.get(click.slot());
    if (jobKey == null) {
      return;
    }

    String playerId = audience.toString();
    String name;
    try {
      name = jobService.getJob(jobKey).getPlainName();
    } catch (IllegalArgumentException e) {
      Messages.send(player, "<error>Job not found: " + jobKey);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      return;
    }
    try {
      if (jobService.getProgression(playerId, jobKey) != null) {
        Messages.send(player, "<neutral>You are already in</neutral> <secondary>" + name + "</secondary>.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        return;
      }
      JoinGate.JoinResult result = joinGate.canJoin(
          player, jobService.getJob(jobKey), jobService.getProgressions(audience));
      if (result != JoinGate.JoinResult.ALLOWED) {
        Messages.send(player, switch (result) {
          case MAX_JOBS -> "<error>You reached the maximum number of jobs you can join.";
          case PERMISSION_DENIED -> "<error>You do not have permission to join</error> <secondary>"
              + name + "</secondary><error>.</error>";
          case WORLD_DENIED -> "<error>You cannot join jobs while in this world.";
          case ALLOWED -> "";
        });
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
        return;
      }
      if (jobService.joinJob(playerId, jobKey)) {
        Messages.send(player,
            "<primary>✓ You joined</primary> <secondary>" + name + "</secondary> <primary>!</primary>");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        inventory.close(audience);
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(JobBrowseGui.class);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
          if (player.isOnline()) {
            open(player);
          }
        }, 1L);
      } else {
        Messages.send(player, "<neutral>You could not join</neutral> <secondary>" + name + "</secondary>.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f);
      }
    } catch (IllegalArgumentException e) {
      Messages.send(player, "<error>Job not found: " + jobKey);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
  }

  /** Builds the craftux inventory view for the current job list (testable pure path). */
  InventoryView buildView(Player player) {
    UUID audience = player.getUniqueId();
    Map<Integer, String> slotJobs = new HashMap<>();

    List<Job> allJobs = jobService.getJobs();
    List<JobProgression> playerJobs = jobService.getProgressions(audience);
    Map<String, JobProgression> playerJobMap = new HashMap<>();
    for (JobProgression prog : playerJobs) {
      playerJobMap.put(prog.job().key().asString(), prog);
    }

    Map<Integer, Slot> content = new HashMap<>();
    int slot = 10;
    int jobIndex = 0;
    for (Job job : allJobs) {
      int row = slot / 9;
      int col = slot % 9;
      if (row >= 5) {
        break;
      }
      if (col == 8) {
        row++;
        col = 1;
        slot = row * 9 + col;
      }
      if (col == 0) {
        col = 1;
        slot = row * 9 + col;
      }
      if (row >= 5 || slot >= GUI_ROWS * 9) {
        break;
      }

      JobProgression progression = playerJobMap.get(job.key().asString());
      content.put(slot, Slot.button(
          "job_" + jobIndex,
          jobItem(job, progression),
          CraftuxUiHost.ACTION_JOB_JOIN,
          SlotPixelIntent.UNVALIDATED));
      slotJobs.put(slot, job.key().asString());
      slot++;
      jobIndex++;
    }

    InventoryView.Builder builder = InventoryView.builder(MENU_ID, GUI_ROWS)
        .title("Browse Jobs");
    ItemSpec pane = CraftuxItems.pane(Material.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < GUI_ROWS * 9; i++) {
      Slot placed = content.get(i);
      if (placed != null) {
        builder.slot(i, placed);
      } else {
        builder.decorative(i, pane);
      }
    }

    sessions.put(audience, Map.copyOf(slotJobs));
    return builder.build();
  }

  private ItemSpec jobItem(Job job, JobProgression progression) {
    boolean isJoined = progression != null;
    Material material = isJoined ? Material.EMERALD : Material.BOOK;
    NamedTextColor nameColor = isJoined ? NamedTextColor.GREEN : NamedTextColor.GOLD;
    String name = PLAIN.serialize(
        job.displayName().color(nameColor).decoration(TextDecoration.ITALIC, false));

    List<String> lore = new ArrayList<>();
    lore.add(plain(job.description().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
    lore.add("");
    lore.add(plain(Component.text()
        .append(Component.text("Max Level: ", NamedTextColor.GRAY))
        .append(Component.text(job.maxLevel(), NamedTextColor.YELLOW))
        .decoration(TextDecoration.ITALIC, false)
        .build()));
    lore.add(plain(Component.text()
        .append(Component.text("Active Players: ", NamedTextColor.GRAY))
        .append(Component.text(countActivePlayers(job), NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .build()));

    Optional<UpgradeTree> treeOpt = upgradeService.getTree(job.key().value());
    if (treeOpt.isPresent()) {
      UpgradeTree tree = treeOpt.get();
      lore.add(plain(Component.text()
          .append(Component.text("Upgrade Tree: ", NamedTextColor.GRAY))
          .append(Component.text(tree.allNodes().size() + " nodes", NamedTextColor.LIGHT_PURPLE))
          .decoration(TextDecoration.ITALIC, false)
          .build()));
    }

    lore.add("");
    lore.add(plain(Component.text("Example Rewards:", NamedTextColor.GOLD)
        .decoration(TextDecoration.ITALIC, false)));
    addExampleRewards(job, lore);

    if (isJoined) {
      lore.add("");
      lore.add(plain(Component.text()
          .append(Component.text("Your Level: ", NamedTextColor.GRAY))
          .append(Component.text(progression.level(), NamedTextColor.GREEN))
          .decoration(TextDecoration.ITALIC, false)
          .build()));
      lore.add(plain(Component.text()
          .append(Component.text("Experience: ", NamedTextColor.GRAY))
          .append(Component.text(progression.experience().toPlainString(), NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .build()));
      lore.add("");
      lore.add(plain(Component.text("✓ Already Joined", NamedTextColor.GREEN)
          .decoration(TextDecoration.ITALIC, false)));
    } else {
      lore.add("");
      lore.add(plain(Component.text("Click to join!", NamedTextColor.YELLOW)
          .decoration(TextDecoration.ITALIC, false)));
    }

    return CraftuxItems.of(material, name, lore);
  }

  private int countActivePlayers(Job job) {
    int count = 0;
    String jobKey = job.key().asString();
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      for (JobProgression prog : jobService.getProgressions(onlinePlayer.getUniqueId())) {
        if (prog.job().key().asString().equals(jobKey)) {
          count++;
          break;
        }
      }
    }
    return count;
  }

  private void addExampleRewards(Job job, List<String> lore) {
    Map<ActionType, List<net.aincraft.JobTask>> allTasks = jobService.getAllTasks(job);
    int examplesAdded = 0;
    int maxExamples = 3;

    for (Map.Entry<ActionType, List<net.aincraft.JobTask>> entry : allTasks.entrySet()) {
      if (examplesAdded >= maxExamples) {
        break;
      }
      List<net.aincraft.JobTask> tasks = entry.getValue();
      if (tasks.isEmpty()) {
        continue;
      }
      net.aincraft.JobTask exampleTask = tasks.get(0);
      List<Payable> payables = exampleTask.payables();
      if (payables.isEmpty()) {
        continue;
      }
      Payable payable = payables.get(0);
      BigDecimal amount = payable.amount().value();
      String payableTypeName = payable.type().key().value();
      String actionName = entry.getKey().key().value();
      if (actionName.contains(":")) {
        actionName = actionName.substring(actionName.indexOf(':') + 1);
      }
      String formattedPayable = formatPayableType(payableTypeName);
      lore.add(plain(Component.text()
          .append(Component.text("  • ", NamedTextColor.DARK_GRAY))
          .append(Component.text(actionName, NamedTextColor.YELLOW))
          .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
          .append(Component.text(amount.toPlainString() + " " + formattedPayable, NamedTextColor.GREEN))
          .decoration(TextDecoration.ITALIC, false)
          .build()));
      examplesAdded++;
    }
    if (examplesAdded == 0) {
      lore.add(plain(Component.text("  No rewards configured", NamedTextColor.GRAY)
          .decoration(TextDecoration.ITALIC, false)));
    }
  }

  private static String formatPayableType(String payableType) {
    if (payableType.contains(":")) {
      payableType = payableType.substring(payableType.indexOf(':') + 1);
    }
    String[] parts = payableType.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(part.substring(0, 1).toUpperCase());
      if (part.length() > 1) {
        result.append(part.substring(1).toLowerCase());
      }
    }
    return result.toString();
  }

  private static String plain(Component component) {
    return PLAIN.serialize(component);
  }
}
