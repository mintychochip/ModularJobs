package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.Job;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

final class JoinCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  @Inject
  public JoinCommand(JobService jobService, JobResolver jobResolver) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("join")
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobResolver.getPlainNames().forEach(builder::suggest);
          return builder.buildFuture();
        }).executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
          }

          String input = context.getArgument("job", String.class);

          // Resolve job (supports both plain name and full key)
          Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);

          if (job == null) {
            // Try fuzzy matching for suggestions
            List<String> suggestions = jobResolver.suggestSimilar(input, 3);

            if (suggestions.isEmpty()) {
              player.sendMessage(Component.text("Job not found: " + input)
                  .color(NamedTextColor.RED));
            } else {
              player.sendMessage(Component.text("Job not found: " + input)
                  .color(NamedTextColor.RED));
              player.sendMessage(Component.text("Did you mean: " + String.join(", ", suggestions))
                  .color(NamedTextColor.GRAY));
            }
            return 0;
          }

          if (jobService.joinJob(player.getUniqueId().toString(), job.key().toString())) {
            player.sendMessage(Component.text("You joined: ")
                .color(NamedTextColor.GREEN)
                .append(job.displayName()));
          } else {
            player.sendMessage(Component.text("You are already in this job.")
                .color(NamedTextColor.YELLOW));
          }

          return Command.SINGLE_SUCCESS;
        }));
  }
}
