package net.aincraft.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
public class DialogNavigationListener implements Listener {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private final InfoCommand infoCommand;
  private static final String NAMESPACE = "modularjobs";
  private static final Key NEXT_KEY = Key.key(NAMESPACE, "info/next");
  private static final Key PREV_KEY = Key.key(NAMESPACE, "info/prev");

  @Inject
  public DialogNavigationListener(JobService jobService, JobResolver jobResolver, InfoCommand infoCommand) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.infoCommand = infoCommand;
  }

  private static final Pattern JOB_NAME_PATTERN = Pattern.compile("jobName:\"([^\"]+)\"");
  private static final Pattern PAGE_PATTERN = Pattern.compile("page:(\\d+)");

  @EventHandler
  public void onPlayerCustomClick(PlayerCustomClickEvent event) {
    Key clickKey = event.getIdentifier();

    if (!clickKey.equals(NEXT_KEY) && !clickKey.equals(PREV_KEY)) {
      return;
    }

    BinaryTagHolder tag = event.getTag();
    if (tag == null) {
      return;
    }

    // Parse SNBT format: {jobName:"miner",page:1}
    String snbt = tag.string();
    String jobName = extractJobName(snbt);
    int currentPage = extractPage(snbt);

    if (jobName == null) {
      return;
    }

    // Cast connection to get player
    if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) {
      return;
    }
    Player player = conn.getPlayer();

    Job job = jobResolver.resolveInNamespace(jobName, NAMESPACE);
    if (job == null) {
      Mint.sendThemedMessage(player, "<error>Job not found!");
      return;
    }

    Map<ActionType, List<JobTask>> tasks = jobService.getAllTasks(job);
    int totalPages = infoCommand.calculateTotalPages(tasks);

    int newPage = clickKey.equals(NEXT_KEY) ? currentPage + 1 : currentPage - 1;

    if (newPage < 1 || newPage > totalPages) {
      return; // Invalid page, do nothing
    }

    Dialog dialog = infoCommand.buildDialog(job, tasks, newPage);
    player.showDialog(dialog);
  }

  private String extractJobName(String snbt) {
    Matcher matcher = JOB_NAME_PATTERN.matcher(snbt);
    return matcher.find() ? matcher.group(1) : null;
  }

  private int extractPage(String snbt) {
    Matcher matcher = PAGE_PATTERN.matcher(snbt);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
  }
}
