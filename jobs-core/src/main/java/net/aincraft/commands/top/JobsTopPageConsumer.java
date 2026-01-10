package net.aincraft.commands.top;

import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.commands.Page;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public interface JobsTopPageConsumer {

  void consume(Component jobName, Page<JobProgression> page, CommandSender sender, int maxPages,
      List<JobProgression> allEntries);
}
