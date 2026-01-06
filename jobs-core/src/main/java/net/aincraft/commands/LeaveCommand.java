package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LeaveCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  @Inject
  public LeaveCommand(JobService jobService, JobResolver jobResolver) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("leave")
        .then(Commands.literal("all")
            .requires(source -> source.getSender().hasPermission("jobs.command.leaveall"))
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();
              if (!(sender instanceof Player player)) {
                Mint.sendMessage(sender, "<error>This command can only be used by players.");
                return 0;
              }

              List<JobProgression> progressions = jobService.getProgressions(player);

              if (progressions.isEmpty()) {
                Mint.sendMessage(player, "<neutral>You are not in any jobs.");
                return 0;
              }

              int leftCount = 0;
              for (JobProgression progression : progressions) {
                if (jobService.leaveJob(player.getUniqueId().toString(), progression.job().key().toString())) {
                  leftCount++;
                }
              }

              if (leftCount == 0) {
                Mint.sendMessage(player, "<error>Failed to leave any jobs.");
              } else if (leftCount == 1) {
                Mint.sendMessage(player, "<primary>✗ You left</primary> <secondary>1 job</secondary> <primary>!</primary>");
              } else {
                Mint.sendMessage(player, "<primary>✗ You left</primary> <secondary>" + leftCount + " jobs</secondary> <primary>!</primary>");
              }

              return Command.SINGLE_SUCCESS;
            })
        )
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobResolver.getPlainNames().forEach(builder::suggest);
          return builder.buildFuture();
        }).executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          if (!(sender instanceof Player player)) {
            Mint.sendMessage(sender, "<error>This command can only be used by players.");
            return Command.SINGLE_SUCCESS;
          }

          String input = context.getArgument("job", String.class);

          // Resolve job (supports both plain name and full key)
          Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);

          if (job == null) {
            // Try fuzzy matching for suggestions
            List<String> suggestions = jobResolver.suggestSimilar(input, 3);

            Mint.sendMessage(player, "<error>Job not found:</error> <secondary>" + input + "</secondary>");
            if (!suggestions.isEmpty()) {
              Mint.sendMessage(player, "<neutral>Did you mean:</neutral> <secondary>" + String.join(", ", suggestions) + "</secondary>");
            }
            return 0;
          }

          if (jobService.leaveJob(player.getUniqueId().toString(), job.key().toString())) {
            Mint.sendMessage(player, "<primary>✗ You left</primary> <secondary>" + job.getPlainName() + "</secondary> <primary>!</primary>");
          } else {
            Mint.sendMessage(player, "<neutral>You are not in</neutral> <secondary>" + job.getPlainName() + "</secondary>.");
          }

          return Command.SINGLE_SUCCESS;
        }));
  }
}
