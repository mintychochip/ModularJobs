package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

public class ListCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public ListCommand(JobService jobService) {
    this.jobService = jobService;
  }

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

  private void displayJobsList(CommandSender sender) {
    List<Job> jobs = jobService.getJobs();

    Mint.sendThemedMessage(sender, "");
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━━━━ <primary>Available Jobs <neutral>━━━━━━━━━");
    Mint.sendThemedMessage(sender, "");

    if (jobs.isEmpty()) {
      Mint.sendThemedMessage(sender, "<neutral>  No jobs are currently available.");
      Mint.sendThemedMessage(sender, "");
    } else {
      PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

      for (Job job : jobs) {
        displayJobEntry(sender, job, serializer);
      }

      Mint.sendThemedMessage(sender, "");
      Mint.sendThemedMessage(sender, "<neutral>  Use <secondary>/jobs join <job><neutral> to join a job.");
      Mint.sendThemedMessage(sender, "<neutral>  Use <secondary>/jobs info <job><neutral> for detailed information.");
    }

    Mint.sendThemedMessage(sender, "");
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Mint.sendThemedMessage(sender, "");
  }

  private void displayJobEntry(CommandSender sender, Job job, PlainTextComponentSerializer serializer) {
    Component jobName = job.displayName();
    Component description = job.description();
    int maxLevel = job.maxLevel();
    String plainName = job.getPlainName();

    // Build hover text with detailed info
    Component hoverText = Mint.createThemedComponent(sender,
        "<primary>Job: <secondary>" + plainName +
        "\n<neutral>Max Level: <accent>" + maxLevel +
        "\n\n<neutral>Click to view details");

    // Build main display: ● JobName (Level X)
    Component mainDisplay = Component.text("  ")
        .append(Mint.createThemedComponent(sender, "<accent>● "))
        .append(jobName)
        .append(Component.space())
        .append(Mint.createThemedComponent(sender, "<neutral>(Level " + maxLevel + ")"))
        .hoverEvent(HoverEvent.showText(hoverText))
        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/jobs info " + plainName));

    sender.sendMessage(mainDisplay);

    // Show description on separate line
    Component descLine = Component.text("    ")
        .append(Mint.createThemedComponent(sender, "<neutral>▸ "))
        .append(description);

    sender.sendMessage(descLine);
  }
}
