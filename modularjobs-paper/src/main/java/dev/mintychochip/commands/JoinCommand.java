package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.domain.JobResolver;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.service.JoinGate.JoinResult;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /jobs join <job>} command: adds the invoking player to the named job. */
public final class JoinCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private final JoinGate joinGate;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  /** Creates the join command with the job service, resolver, and join gate. */
  public JoinCommand(JobService jobService, JobResolver jobResolver, JoinGate joinGate) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.joinGate = joinGate;
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
        .then(
            Commands.argument("job", StringArgumentType.string())
                .suggests(
                    (context, builder) -> {
                      jobResolver.getPlainNames().forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                .executes(
                    context -> {
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
                          Messages.send(
                              player, "<neutral>Did you mean: " + String.join(", ", suggestions));
                        }
                        return 0;
                      }

                      String playerId = player.getUniqueId().toString();
                      if (jobService.getProgression(playerId, job.key().toString()) != null) {
                        Messages.send(
                            player,
                            "<neutral>You are already in</neutral> <secondary>"
                                + job.getPlainName()
                                + "</secondary>.");
                        return Command.SINGLE_SUCCESS;
                      }

                      List<JobProgression> current =
                          jobService.getProgressions(player.getUniqueId());
                      JoinResult result = joinGate.canJoin(player, job, current);
                      switch (result) {
                        case MAX_JOBS ->
                            Messages.send(
                                player,
                                "<error>You reached the maximum number of jobs you can join.");
                        case PERMISSION_DENIED ->
                            Messages.send(
                                player,
                                "<error>You do not have permission to join</error> <secondary>"
                                    + job.getPlainName()
                                    + "</secondary><error>.</error>");
                        case WORLD_DENIED ->
                            Messages.send(
                                player, "<error>You cannot join jobs while in this world.");
                        default -> {
                          // proceed
                        }
                      }
                      if (result != JoinResult.ALLOWED) {
                        return Command.SINGLE_SUCCESS;
                      }

                      if (jobService.joinJob(playerId, job.key().toString())) {
                        Messages.send(
                            player,
                            "<primary>✓ You joined</primary> <secondary>"
                                + job.getPlainName()
                                + "</secondary> <primary>!</primary>");
                      } else {
                        Messages.send(
                            player,
                            "<neutral>You could not join</neutral> <secondary>"
                                + job.getPlainName()
                                + "</secondary>.");
                      }

                      return Command.SINGLE_SUCCESS;
                    }));
  }
}
