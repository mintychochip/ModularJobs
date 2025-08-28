package net.aincraft.commands.top;

import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.kyori.adventure.key.Key;
import org.bukkit.command.CommandSender;

public interface JobsTopPageConsumer {

  void consume(Key jobKey, Page<JobProgression> page, CommandSender sender, int maxPages);
}
