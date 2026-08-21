package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Renders one page of a job leaderboard to a sender. Implementations define
 * the output medium (chat messages, scoreboard, …).
 */
@FunctionalInterface
public interface JobsTopPageConsumer {

  /**
   * Renders the given page to the sender.
   *
   * @param jobName     display name of the job whose top is shown
   * @param page        page of entries to render
   * @param sender      recipient of the rendered output
   * @param maxPages    total page count used for headers and navigation
   * @param allEntries  full cached leaderboard used for context (e.g. viewer rank)
   */
  void consume(Component jobName, Page<JobProgression> page, CommandSender sender, int maxPages,
      List<JobProgression> allEntries);
}
