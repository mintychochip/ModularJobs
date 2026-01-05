package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
                sender.sendMessage(Component.text("Player not found: " + playerName)
                    .color(NamedTextColor.RED));
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
            sender.sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
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
    Component headerText = viewer.equals(target)
        ? Component.text("Your Archived Jobs", NamedTextColor.GOLD, TextDecoration.BOLD)
        : Component.text(targetName + "'s Archived Jobs", NamedTextColor.GOLD, TextDecoration.BOLD);

    viewer.sendMessage(Component.text("━━━━━━━━━ ", NamedTextColor.GRAY)
        .append(headerText)
        .append(Component.text(" ━━━━━━━━━", NamedTextColor.GRAY)));
    viewer.sendMessage(Component.empty());

    if (archivedProgressions.isEmpty()) {
      viewer.sendMessage(Component.text("  No archived jobs found.", NamedTextColor.GRAY));
      viewer.sendMessage(Component.empty());
    } else {
      for (JobProgression progression : archivedProgressions) {
        displayJobEntry(viewer, progression);
      }
    }

    // Footer
    viewer.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));
  }

  private void displayJobEntry(CommandSender viewer, JobProgression progression) {
    int level = progression.level();
    BigDecimal experience = progression.experience();

    viewer.sendMessage(Component.text("  ")
        .append(progression.job().displayName())
        .append(Component.text(" - Level ", NamedTextColor.GRAY))
        .append(Component.text(level, NamedTextColor.GREEN)));

    viewer.sendMessage(Component.text("    Total XP: ", NamedTextColor.GRAY)
        .append(Component.text(formatNumber(experience), NamedTextColor.AQUA)));

    viewer.sendMessage(Component.empty());
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
