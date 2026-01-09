package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.math.BigDecimal;
import net.aincraft.Job;
import net.aincraft.domain.ProgressionService;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.service.JobService;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ExperienceCommand implements JobsCommand {

  private final JobService jobService;
  private final ProgressionService progressionService;

  @Inject
  public ExperienceCommand(JobService jobService, ProgressionService progressionService) {
    this.jobService = jobService;
    this.progressionService = progressionService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("experience")
        .requires(source -> source.getSender().hasPermission("modularjobs.admin"))
        .then(Commands.literal("set")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> executeSet(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("amount", Double.class)))))))
        .then(Commands.literal("add")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> executeAdd(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("amount", Double.class)))))))
        .then(Commands.literal("subtract")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> executeSubtract(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("amount", Double.class)))))));
  }

  private int executeSet(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, double amount) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    BigDecimal newExperience = BigDecimal.valueOf(amount);
    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        newExperience
    );

    if (progressionService.save(newRecord)) {
      Mint.sendThemedMessage(sender, "<primary>✓ Set</primary> <secondary>" + targetPlayer.getName() + "</secondary><primary>'s experience in</primary> <accent>" + job.getPlainName() + "</accent> <primary>to</primary> <secondary>" + String.format("%.2f", amount) + "</secondary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✓ Your experience in</primary> <accent>" + job.getPlainName() + "</accent> <primary>has been set to</primary> <secondary>" + String.format("%.2f", amount) + "</secondary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }

  private int executeAdd(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, double amount) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    BigDecimal newExperience = currentRecord.experience().add(BigDecimal.valueOf(amount));
    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        newExperience
    );

    if (progressionService.save(newRecord)) {
      Mint.sendThemedMessage(sender, "<primary>✓ Added</primary> <secondary>" + String.format("%.2f", amount) + " experience</secondary> <primary>to</primary> <accent>" + targetPlayer.getName() + "</accent> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(total:</primary> <secondary>" + String.format("%.2f", newExperience.doubleValue()) + "</secondary><primary>)</primary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✓ You gained</primary> <secondary>" + String.format("%.2f", amount) + " experience</secondary> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(total:</primary> <secondary>" + String.format("%.2f", newExperience.doubleValue()) + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }

  private int executeSubtract(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, double amount) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    BigDecimal newExperience = currentRecord.experience().subtract(BigDecimal.valueOf(amount));
    if (newExperience.compareTo(BigDecimal.ZERO) < 0) {
      newExperience = BigDecimal.ZERO;
    }

    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        newExperience
    );

    if (progressionService.save(newRecord)) {
      Mint.sendThemedMessage(sender, "<primary>✗ Subtracted</primary> <secondary>" + String.format("%.2f", amount) + " experience</secondary> <primary>from</primary> <accent>" + targetPlayer.getName() + "</accent> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(total:</primary> <secondary>" + String.format("%.2f", newExperience.doubleValue()) + "</secondary><primary>)</primary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✗ You lost</primary> <secondary>" + String.format("%.2f", amount) + " experience</secondary> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(total:</primary> <secondary>" + String.format("%.2f", newExperience.doubleValue()) + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }
}
