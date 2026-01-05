package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.aincraft.service.JobService;
import net.aincraft.service.PerkSyncService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class SubtractLevelCommand implements JobsCommand {

  private final JobService jobService;
  private final ProgressionService progressionService;
  private final PerkSyncService perkSyncService;

  @Inject
  public SubtractLevelCommand(JobService jobService, ProgressionService progressionService,
      PerkSyncService perkSyncService) {
    this.jobService = jobService;
    this.progressionService = progressionService;
    this.perkSyncService = perkSyncService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("subtractlevel")
        .requires(source -> source.getSender().hasPermission("modularjobs.admin"))
        .then(Commands.argument("player", ArgumentTypes.player())
            .then(Commands.argument("job", StringArgumentType.string())
                .suggests((context, builder) -> {
                  jobService.getJobs().stream()
                      .map(job -> job.key().value())
                      .forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(context -> {
                      CommandSourceStack source = context.getSource();
                      CommandSender sender = source.getSender();

                      // Resolve player argument
                      PlayerSelectorArgumentResolver playerResolver = context.getArgument("player",
                          PlayerSelectorArgumentResolver.class);
                      Player targetPlayer = playerResolver.resolve(source).getFirst();

                      if (targetPlayer == null) {
                        sender.sendMessage(
                            Component.text("Player not found.", NamedTextColor.RED));
                        return 0;
                      }

                      // Get job
                      String jobKeyValue = context.getArgument("job", String.class);
                      NamespacedKey jobKey = new NamespacedKey("modularjobs", jobKeyValue);
                      Job job;
                      try {
                        job = jobService.getJob(jobKey.toString());
                      } catch (IllegalArgumentException e) {
                        sender.sendMessage(Component.text("Invalid job: " + jobKeyValue,
                            NamedTextColor.RED));
                        return 0;
                      }

                      // Get amount to subtract
                      int amount = context.getArgument("amount", Integer.class);

                      // Check if player has the job
                      String playerId = targetPlayer.getUniqueId().toString();
                      JobProgressionRecord currentRecord = progressionService.load(playerId,
                          jobKey.toString());

                      if (currentRecord == null) {
                        sender.sendMessage(Component.text(
                            targetPlayer.getName() + " is not in job " + job.getPlainName(),
                            NamedTextColor.RED));
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
                        sender.sendMessage(Component.text(
                            targetPlayer.getName() + " is already at level 1 for job "
                                + job.getPlainName(),
                            NamedTextColor.RED));
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
                        int levelsSubtracted = currentLevel - newLevel;

                        // Revoke perks above the new level
                        perkSyncService.revokePerksAboveLevel(targetPlayer, job, newLevel);

                        sender.sendMessage(Component.text(
                            "Subtracted " + levelsSubtracted + " level(s) from "
                                + targetPlayer.getName() + " in " + job.getPlainName()
                                + " (now level " + newLevel + ")",
                            NamedTextColor.GREEN));

                        // Notify target player if online
                        targetPlayer.sendMessage(Component.text(
                            "You lost " + levelsSubtracted + " level(s) in " + job.getPlainName()
                                + " (now level " + newLevel + ")",
                            NamedTextColor.GOLD));

                        return Command.SINGLE_SUCCESS;
                      } else {
                        sender.sendMessage(Component.text(
                            "Failed to update progression. Please check server logs.",
                            NamedTextColor.RED));
                        return 0;
                      }
                    }))));
  }
}
