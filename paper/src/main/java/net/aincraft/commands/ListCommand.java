package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aincraft.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

/** {@code /jobs list} command: prints the available jobs with hover/click details. */
public class ListCommand implements JobsCommand {

  private final JobService jobService;

  /** Creates the list command with the job service that enumerates available jobs. */
  public ListCommand(JobService jobService) {
    this.jobService = jobService;
  }

  /**
   * Builds the {@code /jobs list} command (permission-gated) that renders the available jobs.
   *
   * @return the Brigadier command tree for listing jobs
   */
  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("list")
        .requires(source -> source.getSender().hasPermission("jobs.command.list"))
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          displayJobsList(sender);
          return Command.SINGLE_SUCCESS;
        });
  }

  /** Sends the sender the list of available jobs with header, entries, and usage footer. */
  private void displayJobsList(CommandSender sender) {
    List<Job> jobs = jobService.getJobs();

    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━━━━ <primary>Available Jobs <neutral>━━━━━━━━━");
    Messages.send(sender, "");

    if (jobs.isEmpty()) {
      Messages.send(sender, "<neutral>  No jobs are currently available.");
      Messages.send(sender, "");
    } else {
      PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

      for (Job job : jobs) {
        displayJobEntry(sender, job, serializer);
      }

      Messages.send(sender, "");
      Messages.send(sender, "<neutral>  Use <secondary>/jobs join <job><neutral> to join a job.");
      Messages.send(sender, "<neutral>  Use <secondary>/jobs info <job><neutral> for detailed information.");
    }

    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(sender, "");
  }

  /** Sends one job entry line (with hover details and click-to-view) plus its description. */
  private void displayJobEntry(CommandSender sender, Job job, PlainTextComponentSerializer serializer) {
    Component jobName = job.displayName();
    Component description = job.description();
    int maxLevel = job.maxLevel();
    String plainName = job.getPlainName();

    // Build hover text with detailed info
    Component hoverText = Messages.component("<primary>Job: <secondary>" + plainName +
        "\n<neutral>Max Level: <accent>" + maxLevel +
        "\n\n<neutral>Click to view details");

    // Build main display: ● JobName (Level X)
    Component mainDisplay = Component.text("  ")
        .append(Messages.component("<accent>● "))
        .append(jobName)
        .append(Component.space())
        .append(Messages.component("<neutral>(Level " + maxLevel + ")"))
        .hoverEvent(HoverEvent.showText(hoverText))
        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/jobs info " + plainName));

    sender.sendMessage(mainDisplay);

    // Show description on separate line
    Component descLine = Component.text("    ")
        .append(Messages.component("<neutral>▸ "))
        .append(description);

    sender.sendMessage(descLine);
  }
}
