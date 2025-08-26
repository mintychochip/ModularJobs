package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

//TODO: before the server starts, lets query the info from the repository before we start
public class InfoCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public InfoCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("info")
        .then(Commands.argument("job", ArgumentTypes.key())
            .then(Commands.argument("pageNumber", IntegerArgumentType.integer(1))
                .executes(context -> {
                  CommandSourceStack source = context.getSource();
                  CommandSender sender = source.getSender();

                  Key jobKey = context.getArgument("job", Key.class);
                  int page = IntegerArgumentType.getInteger(context, "pageNumber");

                  Job job = jobService.getJob(jobKey.toString());
                  if (job == null) {
                    sender.sendMessage("The job you specified does not exist.");
                    return 0;
                  }

                  Map<ActionType, List<JobTask>> tasks = jobService.getAllTasks(job);
                  for (var entry : tasks.entrySet()) {
                    var type = entry.getKey();
                    var value = entry.getValue();
                    if (value.isEmpty()) continue;

                    sender.sendMessage(type.toString());
                    for (JobTask task : value) {
                      TextComponent text = Component.text(task.getContext().toString()).append(Component.text("->"));
                      for (Payable payable : task.getPayables()) {
                        text = text.append(payable).append(Component.space());
                      }
                      sender.sendMessage(Component.text("  ").append(text));
                    }
                  }
                  return 1;
                })
            )
        );
  }

}
