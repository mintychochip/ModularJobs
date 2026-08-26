package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.Job;
import dev.mintychochip.service.JobService;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;

/** {@code /jobs list} command: prints the available jobs with hover/click details. */
public class ListCommand implements JobsCommand {

  private final JobService jobService;

  /** List command. */
  public ListCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("list")
        .requires(source -> source.getSender().hasPermission("jobs.command.list"))
        .executes(
            context -> {
              displayJobsList(context.getSource().getSender());
              return Command.SINGLE_SUCCESS;
            });
  }

  private void displayJobsList(CommandSender sender) {
    final List<Job> jobs = jobService.getJobs();
    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━━━━ <primary>Available Jobs <neutral>━━━━━━━━━");
    Messages.send(sender, "");
    if (jobs.isEmpty()) {
      Messages.send(sender, "<neutral>  No jobs are currently available.");
      Messages.send(sender, "");
    } else {
      for (Job job : jobs) {
        displayJobEntry(sender, job);
      }
      Messages.send(sender, "");
      Messages.send(sender, "<neutral>  Use <secondary>/jobs join <job><neutral> to join a job.");
      Messages.send(
          sender, "<neutral>  Use <secondary>/jobs info <job><neutral> for detailed information.");
    }
    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(sender, "");
  }

  private void displayJobEntry(CommandSender sender, Job job) {
    Component jobName = job.displayName();
    Component description = job.description();
    int maxLevel = job.maxLevel();
    String plainName = job.getPlainName();
    Component hoverText =
        Messages.component(
            "<primary>Job: <secondary>"
                + plainName
                + "\n<neutral>Max Level: <accent>"
                + maxLevel
                + "\n\n<neutral>Click to view details");
    Component mainDisplay =
        Component.text("  ")
            .append(Messages.component("<accent>● "))
            .append(jobName)
            .append(Component.space())
            .append(Messages.component("<neutral>(Level " + maxLevel + ")"))
            .hoverEvent(HoverEvent.showText(hoverText))
            .clickEvent(
                net.kyori.adventure.text.event.ClickEvent.runCommand("/jobs info " + plainName));
    sender.sendMessage(mainDisplay);
    sender.sendMessage(
        Component.text("    ").append(Messages.component("<neutral>▸ ")).append(description));
  }
}
