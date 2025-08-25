package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.service.JobService;
import net.aincraft.service.ProgressionService;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class JoinCommand implements JobsCommand {

  private final ProgressionService progressionService;
  private final JobService jobService;

  @Inject
  public JoinCommand(ProgressionService progressionService,
      JobService jobService) {
    this.progressionService = progressionService;
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("join")
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobService.getJobs().stream().map(job -> job.key().value()).forEach(builder::suggest);
          return builder.buildFuture();
        }).executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          if (!(sender instanceof Player player)) {
            //TODO: add admin
            sender.sendMessage("failed to execute command");
            return Command.SINGLE_SUCCESS;
          }
          NamespacedKey jobKey = new NamespacedKey("modularjobs",context.getArgument("job", String.class));
          Optional<Job> job = jobService.getJob(jobKey);
          if (job.isEmpty()) {
            player.sendMessage("invalid job");
            return 0;
          }
          try {
            progressionService.create(player, job.get());
          } catch (IllegalArgumentException ex) {
            player.sendMessage("You are already in this job.");
          }
          player.sendMessage(Component.text("You joined: ").append(job.get().getDisplayName()));
          return Command.SINGLE_SUCCESS;
        }));
  }
}
