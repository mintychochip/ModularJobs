package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LeaveAllCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public LeaveAllCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("leaveall")
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
            Mint.sendMessage(player, "<success>You left 1 job.");
          } else {
            Mint.sendMessage(player, "<success>You left " + leftCount + " jobs.");
          }

          return Command.SINGLE_SUCCESS;
        });
  }
}
