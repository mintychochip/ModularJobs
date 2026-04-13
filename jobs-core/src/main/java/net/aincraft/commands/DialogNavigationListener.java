package net.aincraft.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.aincraft.service.PreferencesService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class DialogNavigationListener implements Listener {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private final InfoCommand infoCommand;
  private final StatsDialog statsDialog;
  private final PreferencesService preferencesService;

  private static final String NAMESPACE = "modularjobs";
  // Info dialog navigation keys
  private static final Key INFO_NEXT_KEY = Key.key(NAMESPACE, "info/next");
  private static final Key INFO_PREV_KEY = Key.key(NAMESPACE, "info/prev");
  // Stats dialog navigation keys
  private static final Key STATS_NEXT_KEY = Key.key(NAMESPACE, "stats/next");
  private static final Key STATS_PREV_KEY = Key.key(NAMESPACE, "stats/prev");

  @Inject
  public DialogNavigationListener(JobService jobService, JobResolver jobResolver, 
      InfoCommand infoCommand, StatsDialog statsDialog, PreferencesService preferencesService) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.infoCommand = infoCommand;
    this.statsDialog = statsDialog;
    this.preferencesService = preferencesService;
  }

  private static final Pattern JOB_NAME_PATTERN = Pattern.compile("jobName:\"([^\"]+)\"");
  private static final Pattern UUID_PATTERN = Pattern.compile("uuid:\"([^\"]+)\"");
  private static final Pattern PAGE_PATTERN = Pattern.compile("page:(\\d+)");

  @EventHandler
  public void onPlayerCustomClick(PlayerCustomClickEvent event) {
    Key clickKey = event.getIdentifier();

    // Handle info dialog navigation
    if (clickKey.equals(INFO_NEXT_KEY) || clickKey.equals(INFO_PREV_KEY)) {
      handleInfoNavigation(event, clickKey);
      return;
    }

    // Handle stats dialog navigation
    if (clickKey.equals(STATS_NEXT_KEY) || clickKey.equals(STATS_PREV_KEY)) {
      handleStatsNavigation(event, clickKey);
    }
  }

  private void handleInfoNavigation(PlayerCustomClickEvent event, Key clickKey) {
    BinaryTagHolder tag = event.getTag();
    if (tag == null) {
      return;
    }

    String snbt = tag.string();
    String jobName = extractJobName(snbt);
    int currentPage = extractPage(snbt);

    if (jobName == null) {
      return;
    }

    if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) {
      return;
    }
    Player player = conn.getPlayer();

    Job job = jobResolver.resolveInNamespace(jobName, NAMESPACE);
    if (job == null) {
      Mint.sendThemedMessage(player, "<error>Job not found!");
      return;
    }

    int entriesPerPage = preferencesService.getEntriesPerPage(player);
    Map<ActionType, List<JobTask>> tasks = jobService.getAllTasks(job);
    int totalPages = infoCommand.calculateTotalPages(tasks, entriesPerPage);

    int newPage = clickKey.equals(INFO_NEXT_KEY) ? currentPage + 1 : currentPage - 1;

    if (newPage < 1 || newPage > totalPages) {
      return;
    }

    Dialog dialog = infoCommand.buildDialog(job, tasks, newPage, entriesPerPage);
    player.showDialog(dialog);
  }

  private void handleStatsNavigation(PlayerCustomClickEvent event, Key clickKey) {
    BinaryTagHolder tag = event.getTag();
    if (tag == null) {
      return;
    }

    String snbt = tag.string();
    String uuidStr = extractUuid(snbt);
    int currentPage = extractPage(snbt);

    if (uuidStr == null) {
      return;
    }

    if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) {
      return;
    }
    Player player = conn.getPlayer();

    UUID uuid;
    try {
      uuid = UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      return;
    }

    OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
    List<JobProgression> progressions = jobService.getProgressions(target);
    int totalPages = StatsDialog.calculateTotalPages(progressions);

    int newPage = clickKey.equals(STATS_NEXT_KEY) ? currentPage + 1 : currentPage - 1;

    if (newPage < 1 || newPage > totalPages) {
      return;
    }

    Dialog dialog = statsDialog.buildDialog(target, progressions, newPage);
    player.showDialog(dialog);
  }

  private String extractJobName(String snbt) {
    Matcher matcher = JOB_NAME_PATTERN.matcher(snbt);
    return matcher.find() ? matcher.group(1) : null;
  }

  private String extractUuid(String snbt) {
    Matcher matcher = UUID_PATTERN.matcher(snbt);
    return matcher.find() ? matcher.group(1) : null;
  }

  private int extractPage(String snbt) {
    Matcher matcher = PAGE_PATTERN.matcher(snbt);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
  }
}
