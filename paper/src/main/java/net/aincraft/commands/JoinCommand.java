package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aincraft.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.Job;
import net.aincraft.domain.JobResolver;
import net.aincraft.service.JobService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /jobs join <job>} command: adds the invoking player to the named job. */
public final class JoinCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  /** Creates the join command with the job service and resolver used to look up jobs. */
  public JoinCommand(JobService jobService, JobResolver jobResolver) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  /**
   * Builds the {@code /jobs join} command with a job argument validated against the resolver and,
   * when the job is unknown, offered as fuzzy "did you mean" suggestions.
   *
   * @return the Brigadier command tree for joining a job
   */
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
            Messages.send(sender, "<error>This command can only be used by players.");
            return Command.SINGLE_SUCCESS;
          }

          String input = context.getArgument("job", String.class);

          // Resolve job (supports both plain name and full key)
          Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);

          if (job == null) {
            // Try fuzzy matching for suggestions
            List<String> suggestions = jobResolver.suggestSimilar(input, 3);

            Messages.send(player, "<error>Job not found: " + input);
            if (!suggestions.isEmpty()) {
              Messages.send(player, "<neutral>Did you mean: " + String.join(", ", suggestions));
            }
            return 0;
          }

          if (jobService.joinJob(player.getUniqueId().toString(), job.key().toString())) {
            Messages.send(player, "<primary>✓ You joined</primary> <secondary>" + job.getPlainName() + "</secondary> <primary>!</primary>");
          } else {
            Messages.send(player, "<neutral>You are already in</neutral> <secondary>" + job.getPlainName() + "</secondary>.");
          }

          return Command.SINGLE_SUCCESS;
        }));
  }
}
