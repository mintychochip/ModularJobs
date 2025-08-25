package net.aincraft.commands.top;

import java.util.List;
import java.util.Map;
import net.aincraft.JobTask;
import net.aincraft.commands.top.PageConsumer.Page;
import net.aincraft.container.ActionType;
import org.bukkit.command.CommandSender;

public class ChatJobsInfoPageConsumerImpl implements PageConsumer<Map<ActionType, List<JobTask>>> {

  @Override
  public void consume(Page<Map<ActionType, List<JobTask>>> page, CommandSender sender) {

  }
}
