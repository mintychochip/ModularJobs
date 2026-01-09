package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArchiveCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public ArchiveCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("archive")
        // /jobs archive <playerName> - admin variant
        .then(Commands.argument("player", StringArgumentType.word())
            .requires(source -> source.getSender().hasPermission("jobs.command.admin.archive"))
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              String playerName = context.getArgument("player", String.class);
              OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

              if (target == null) {
                Mint.sendThemedMessage(sender, "<error>Player not found: " + playerName);
                return 0;
              }

              displayArchive(sender, target);
              return Command.SINGLE_SUCCESS;
            }))
        // /jobs archive - player variant
        .requires(source -> source.getSender().hasPermission("jobs.command.archive"))
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          if (!(sender instanceof Player player)) {
            Mint.sendThemedMessage(sender, "<error>This command can only be used by players.");
            return 0;
          }

          displayArchive(player, player);
          return Command.SINGLE_SUCCESS;
        });
  }

  private void displayArchive(CommandSender viewer, OfflinePlayer target) {
    List<JobProgression> archivedProgressions = jobService.getArchivedProgressions(target);

    // Header
    String targetName = target.getName() != null ? target.getName() : "Unknown";
    String header = viewer.equals(target)
        ? "<primary>Your Archived Jobs"
        : "<primary>" + targetName + "'s Archived Jobs";

    Mint.sendThemedMessage(viewer, "<neutral>━━━━━━━━━ " + header + " <neutral>━━━━━━━━━");

    if (archivedProgressions.isEmpty()) {
      Mint.sendThemedMessage(viewer, "<neutral>  No archived jobs found.");
    } else {
      for (JobProgression progression : archivedProgressions) {
        displayJobEntry(viewer, progression);
      }
    }

    // Footer
    Mint.sendThemedMessage(viewer, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  }

  private void displayJobEntry(CommandSender viewer, JobProgression progression) {
    int level = progression.level();
    BigDecimal experience = progression.experience();

    Mint.sendThemedMessage(viewer, "  " + progression.job().getPlainName() + " <neutral>- Level <accent>" + level);
    Mint.sendThemedMessage(viewer, "    <neutral>Total XP: <accent>" + formatNumber(experience));
  }

  private String formatNumber(BigDecimal number) {
    if (number.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
      return number.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP) + "M";
    } else if (number.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
      return number.divide(BigDecimal.valueOf(1_000), 2, RoundingMode.HALF_UP) + "K";
    } else {
      return number.setScale(0, RoundingMode.HALF_UP).toString();
    }
  }
}
