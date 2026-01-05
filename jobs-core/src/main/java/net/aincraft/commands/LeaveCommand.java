package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.Job;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public LeaveCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("leave")
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobService.getJobs().stream().map(job -> job.key().value()).forEach(builder::suggest);
          return builder.buildFuture();
        }).executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          if (!(sender instanceof Player player)) {
            //TODO: add admin
            sender.sendMessage("failed to execute command");
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
          }
          NamespacedKey jobKey = new NamespacedKey("modularjobs",context.getArgument("job", String.class));
          Job job = jobService.getJob(jobKey.toString());
          if (job == null) {
            player.sendMessage("invalid job");
            return 0;
          }
          if (jobService.leaveJob(player.getUniqueId().toString(), jobKey.toString())) {
            player.sendMessage(Component.text("You left: ").append(job.displayName()));
          }
          return Command.SINGLE_SUCCESS;
        }));
  }
}
