package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.math.BigDecimal;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.domain.ProgressionService;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.event.JobLevelEvent;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Unified command for managing player job levels.
 *
 * Usage:
 * - /jobs level set <player> <job> <level>
 * - /jobs level add <player> <job> <amount>
 * - /jobs level subtract <player> <job> <amount>
 */
public final class LevelCommand implements JobsCommand {

  private final JobService jobService;
  private final ProgressionService progressionService;

  @Inject
  public LevelCommand(JobService jobService, ProgressionService progressionService) {
    this.jobService = jobService;
    this.progressionService = progressionService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("level")
        .requires(source -> source.getSender().hasPermission("modularjobs.admin"))
        // /jobs level set
        .then(Commands.literal("set")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("level", IntegerArgumentType.integer(1))
                        .executes(context -> executeSet(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("level", Integer.class)))))))
        // /jobs level add
        .then(Commands.literal("add")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> executeAdd(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("amount", Integer.class)))))))
        // /jobs level subtract
        .then(Commands.literal("subtract")
            .then(Commands.argument("player", ArgumentTypes.player())
                .then(Commands.argument("job", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      jobService.getJobs().stream()
                          .map(job -> job.key().value())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> executeSubtract(
                            context.getSource(),
                            context.getArgument("player", PlayerSelectorArgumentResolver.class),
                            context.getArgument("job", String.class),
                            context.getArgument("amount", Integer.class)))))));
  }

  private int executeSet(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, int targetLevel) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    // Get job
    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    // Validate level is within bounds
    if (targetLevel > job.maxLevel()) {
      Mint.sendThemedMessage(sender, "<error>Level</error> <secondary>" + targetLevel + "</secondary> <error>exceeds max level</error> <secondary>" + job.maxLevel() + "</secondary> <error>for job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Check if player has the job
    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Get current level before change
    List<JobProgression> progressions = jobService.getProgressions(targetPlayer);
    int oldLevel = progressions.stream()
        .filter(p -> p.job().key().toString().equals(jobKey.toString()))
        .findFirst()
        .map(JobProgression::level)
        .orElse(1);

    // Calculate required experience for target level
    BigDecimal requiredExperience = job.levelingCurve()
        .evaluate(new net.aincraft.LevelingCurve.Parameters(targetLevel));

    // Create new progression record with updated experience
    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        requiredExperience
    );

    // Save the updated progression
    if (progressionService.save(newRecord)) {
      // Fire JobLevelEvent so listeners (like UpgradeLevelUpListener) can handle level changes
      if (targetLevel != oldLevel) {
        JobProgression updatedProgression = jobService.getProgression(playerId, jobKey.toString());
        if (updatedProgression != null) {
          JobLevelEvent event = new JobLevelEvent(targetPlayer, job, oldLevel, targetLevel, updatedProgression, JobLevelEvent.Reason.ADMIN_COMMAND);
          Bukkit.getPluginManager().callEvent(event);
        }
      }

      Mint.sendThemedMessage(sender, "<primary>✓ Set</primary> <secondary>" + targetPlayer.getName() + "</secondary><primary>'s level in</primary> <accent>" + job.getPlainName() + "</accent> <primary>to</primary> <secondary>" + targetLevel + "</secondary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✓ Your level in</primary> <accent>" + job.getPlainName() + "</accent> <primary>has been set to</primary> <secondary>" + targetLevel + "</secondary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }

  private int executeAdd(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, int amount) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    // Get job
    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    // Check if player has the job
    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Get current level from JobProgression
    List<JobProgression> progressions = jobService.getProgressions(targetPlayer);
    int currentLevel = progressions.stream()
        .filter(p -> p.job().key().toString().equals(jobKey.toString()))
        .findFirst()
        .map(JobProgression::level)
        .orElse(1);

    // Calculate new level, capped at max level
    int newLevel = Math.min(currentLevel + amount, job.maxLevel());

    if (newLevel == currentLevel) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is already at max level</error> <secondary>" + job.maxLevel() + "</secondary> <error>for job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Calculate required experience for new level
    BigDecimal requiredExperience = job.levelingCurve()
        .evaluate(new net.aincraft.LevelingCurve.Parameters(newLevel));

    // Create new progression record with updated experience
    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        requiredExperience
    );

    // Save the updated progression
    if (progressionService.save(newRecord)) {
      // Fire JobLevelEvent so listeners (like UpgradeLevelUpListener) can handle level changes
      int levelsAdded = newLevel - currentLevel;
      if (levelsAdded > 0) {
        JobProgression updatedProgression = jobService.getProgression(playerId, jobKey.toString());
        if (updatedProgression != null) {
          JobLevelEvent event = new JobLevelEvent(targetPlayer, job, currentLevel, newLevel, updatedProgression, JobLevelEvent.Reason.ADMIN_COMMAND);
          Bukkit.getPluginManager().callEvent(event);
        }
      }

      Mint.sendThemedMessage(sender, "<primary>✓ Added</primary> <secondary>" + levelsAdded + " level(s)</secondary> <primary>to</primary> <accent>" + targetPlayer.getName() + "</accent> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(now level</primary> <secondary>" + newLevel + "</secondary><primary>)</primary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✓ You gained</primary> <secondary>" + levelsAdded + " level(s)</secondary> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(now level</primary> <secondary>" + newLevel + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }

  private int executeSubtract(CommandSourceStack source, PlayerSelectorArgumentResolver playerResolver,
      String jobKeyValue, int amount) throws CommandSyntaxException {
    CommandSender sender = source.getSender();
    Player targetPlayer = playerResolver.resolve(source).getFirst();

    if (targetPlayer == null) {
      Mint.sendThemedMessage(sender, "<error>Player not found.</error>");
      return 0;
    }

    // Get job
    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
    Job job;
    try {
      job = jobService.getJob(jobKey.toString());
    } catch (IllegalArgumentException e) {
      Mint.sendThemedMessage(sender, "<error>Invalid job:</error> <secondary>" + jobKeyValue + "</secondary>");
      return 0;
    }

    // Check if player has the job
    String playerId = targetPlayer.getUniqueId().toString();
    JobProgressionRecord currentRecord = progressionService.load(playerId, jobKey.toString());

    if (currentRecord == null) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is not in job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Get current level from JobProgression
    List<JobProgression> progressions = jobService.getProgressions(targetPlayer);
    int currentLevel = progressions.stream()
        .filter(p -> p.job().key().toString().equals(jobKey.toString()))
        .findFirst()
        .map(JobProgression::level)
        .orElse(1);

    // Calculate new level, floored at level 1
    int newLevel = Math.max(currentLevel - amount, 1);

    if (newLevel == currentLevel) {
      Mint.sendThemedMessage(sender, "<secondary>" + targetPlayer.getName() + "</secondary> <error>is already at level 1 for job</error> <accent>" + job.getPlainName() + "</accent>");
      return 0;
    }

    // Calculate required experience for new level
    BigDecimal requiredExperience = job.levelingCurve()
        .evaluate(new net.aincraft.LevelingCurve.Parameters(newLevel));

    // Create new progression record with updated experience
    JobProgressionRecord newRecord = new JobProgressionRecord(
        playerId,
        currentRecord.jobRecord(),
        requiredExperience
    );

    // Save the updated progression
    if (progressionService.save(newRecord)) {
      // Fire JobLevelEvent so listeners (like UpgradeLevelUpListener) can handle level changes
      int levelsSubtracted = currentLevel - newLevel;
      if (levelsSubtracted > 0) {
        JobProgression updatedProgression = jobService.getProgression(playerId, jobKey.toString());
        if (updatedProgression != null) {
          JobLevelEvent event = new JobLevelEvent(targetPlayer, job, currentLevel, newLevel, updatedProgression, JobLevelEvent.Reason.ADMIN_COMMAND);
          Bukkit.getPluginManager().callEvent(event);
        }
      }

      Mint.sendThemedMessage(sender, "<primary>✗ Subtracted</primary> <secondary>" + levelsSubtracted + " level(s)</secondary> <primary>from</primary> <accent>" + targetPlayer.getName() + "</accent> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(now level</primary> <secondary>" + newLevel + "</secondary><primary>)</primary>");
      Mint.sendThemedMessage(targetPlayer, "<primary>✗ You lost</primary> <secondary>" + levelsSubtracted + " level(s)</secondary> <primary>in</primary> <accent>" + job.getPlainName() + "</accent> <primary>(now level</primary> <secondary>" + newLevel + "</secondary><primary>)</primary>");
      return Command.SINGLE_SUCCESS;
    } else {
      Mint.sendThemedMessage(sender, "<error>Failed to update progression. Please check server logs.</error>");
      return 0;
    }
  }
}
