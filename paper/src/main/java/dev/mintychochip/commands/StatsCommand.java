package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.JobProgression;
import dev.mintychochip.gui.StatsGui;
import dev.mintychochip.service.JobService;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Stats command. */
public class StatsCommand implements JobsCommand {

  private final JobService jobService;
  private final StatsGui statsGui;

  /** Stats command. */
  public StatsCommand(JobService jobService, StatsGui statsGui) {
    this.jobService = jobService;
    this.statsGui = statsGui;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("stats")
        // /jobs stats chat <playerName> - admin variant (chat output)
        .then(
            Commands.literal("chat")
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .requires(
                            source -> source.getSender().hasPermission("jobs.command.admin.stats"))
                        .executes(
                            context -> {
                              CommandSourceStack source = context.getSource();
                              CommandSender sender = source.getSender();

                              String playerName = context.getArgument("player", String.class);
                              OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

                              if (target == null) {
                                Messages.send(sender, "<error>Player not found: " + playerName);
                                return 0;
                              }

                              displayStatsChat(sender, target);
                              return Command.SINGLE_SUCCESS;
                            }))
                // /jobs stats chat - player variant (chat output)
                .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
                .executes(
                    context -> {
                      CommandSourceStack source = context.getSource();
                      CommandSender sender = source.getSender();

                      if (!(sender instanceof Player player)) {
                        Messages.send(sender, "<error>This command can only be used by players.");
                        return 0;
                      }

                      displayStatsChat(player, player);
                      return Command.SINGLE_SUCCESS;
                    }))
        // /jobs stats <playerName> - admin variant (GUI output)
        .then(
            Commands.argument("player", StringArgumentType.word())
                .requires(source -> source.getSender().hasPermission("jobs.command.admin.stats"))
                .executes(
                    context -> {
                      CommandSourceStack source = context.getSource();
                      CommandSender sender = source.getSender();

                      String playerName = context.getArgument("player", String.class);
                      OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

                      if (target == null) {
                        Messages.send(sender, "<error>Player not found: " + playerName);
                        return 0;
                      }

                      displayStats(sender, target);
                      return Command.SINGLE_SUCCESS;
                    }))
        // /jobs stats - player variant (GUI output)
        .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
        .executes(
            context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                Messages.send(sender, "<error>This command can only be used by players.");
                return 0;
              }

              displayStats(player, player);
              return Command.SINGLE_SUCCESS;
            });
  }

  /** Displays stats in a GUI dialog. Falls back to chat if sender is not a player. */
  private void displayStats(CommandSender viewer, OfflinePlayer target) {
    if (viewer instanceof Player player) {
      displayStatsDialog(player, target);
    } else {
      displayStatsChat(viewer, target);
    }
  }

  /** Displays stats in a craftux inventory GUI. */
  private void displayStatsDialog(Player viewer, OfflinePlayer target) {
    final List<JobProgression> progressions = jobService.getProgressions(target.getUniqueId());
    statsGui.open(viewer, target, progressions, 1);
  }

  /** Displays stats in chat format (original implementation). */
  private void displayStatsChat(CommandSender viewer, OfflinePlayer target) {
    final List<JobProgression> progressions = jobService.getProgressions(target.getUniqueId());

    String targetName = target.getName() != null ? target.getName() : "Unknown";
    String header =
        viewer.equals(target)
            ? "<primary>Job Statistics"
            : "<primary>" + targetName + "'s Job Statistics";

    Messages.send(viewer, "");
    Messages.send(viewer, "<neutral>━━━━━━━━━ " + header + " <neutral> ━━━━━━━━━");
    Messages.send(viewer, "");

    if (progressions.isEmpty()) {
      String message =
          viewer.equals(target)
              ? "<neutral>  You are not in any jobs."
              : "<neutral>  " + targetName + " is not in any jobs.";
      Messages.send(viewer, message);
      Messages.send(viewer, "<neutral>  Use <secondary>/jobs join<neutral> to join a job.");
      Messages.send(viewer, "");
    } else {
      for (JobProgression progression : progressions) {
        displayJobStats(viewer, progression);
      }
    }

    Messages.send(viewer, "");
    Messages.send(viewer, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(viewer, "");
  }

  private void displayJobStats(CommandSender viewer, JobProgression progression) {
    int currentLevel = progression.level();
    BigDecimal currentXp = progression.experience();
    int maxLevel = progression.job().maxLevel();

    // Calculate percentage and XP values for display
    double percentage;
    BigDecimal xpCurrent;
    BigDecimal xpTotal;

    if (currentLevel >= maxLevel) {
      percentage = 100.0;
      xpCurrent = currentXp;
      xpTotal = null; // MAX level
    } else {
      BigDecimal currentLevelXp = progression.experienceForLevel(currentLevel);
      BigDecimal nextLevelXp = progression.experienceForLevel(currentLevel + 1);
      xpCurrent = currentXp.subtract(currentLevelXp);
      xpTotal = nextLevelXp.subtract(currentLevelXp);
      percentage =
          xpCurrent
              .divide(xpTotal, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue();
    }

    // Build hover text with all metadata
    Component hoverText =
        buildHoverText(currentLevel, maxLevel, currentXp, xpCurrent, xpTotal, percentage);

    // Build main display: bar + Lvl. [level] + [name]
    String bar = createProgressBar(percentage);
    Component barComponent = Messages.component(bar);
    Component jobName = progression.job().displayName();
    Component mainDisplay =
        Component.text("  ")
            .append(barComponent)
            .append(Component.space())
            .append(Messages.component("<neutral>Lvl. "))
            .append(Messages.component("<secondary>" + currentLevel))
            .append(Component.space())
            .append(jobName)
            .hoverEvent(HoverEvent.showText(hoverText));

    viewer.sendMessage(mainDisplay);
  }

  private Component buildHoverText(
      int currentLevel,
      int maxLevel,
      BigDecimal currentXp,
      BigDecimal xpCurrent,
      BigDecimal xpTotal,
      double percentage) {
    String percentageStr = String.format("%.1f%%", percentage);
    String xpCurrentStr = formatFullNumber(xpCurrent);
    String xpTotalStr = xpTotal != null ? formatFullNumber(xpTotal) : "MAX";
    String totalXpStr = formatFullNumber(currentXp);
    String progressColor = getProgressColorTag(percentage);

    return Messages.component(
        "<neutral>Level: <primary>"
            + currentLevel
            + " / "
            + maxLevel
            + "\n<neutral>Progress: <"
            + progressColor
            + ">"
            + percentageStr
            + "\n<neutral>XP in level: <secondary>"
            + xpCurrentStr
            + " / "
            + xpTotalStr
            + "\n<neutral>Total XP: <accent>"
            + totalXpStr);
  }

  private String createProgressBar(double percentage) {
    int barLength = 32;
    int filled = (int) Math.round(percentage / 100.0 * barLength);
    filled = Math.min(barLength, Math.max(0, filled));

    String colorTag = getProgressColorTag(percentage);
    StringBuilder bar = new StringBuilder("<neutral>[");

    for (int i = 0; i < barLength; i++) {
      if (i < filled) {
        bar.append("<").append(colorTag).append(">|");
      } else {
        bar.append("<neutral>|");
      }
    }

    bar.append("<neutral>]");
    return bar.toString();
  }

  private String getProgressColorTag(double percentage) {
    if (percentage >= 75) {
      return "accent"; // Aqua
    } else if (percentage >= 50) {
      return "secondary"; // Yellow
    } else if (percentage >= 25) {
      return "primary"; // Gold
    } else {
      return "error"; // Red
    }
  }

  private String formatFullNumber(BigDecimal number) {
    return String.format("%,d", number.setScale(0, RoundingMode.HALF_UP).intValue());
  }
}
