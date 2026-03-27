package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.mintychochip.mint.Mint;

public class StatsCommand implements JobsCommand {

  private final JobService jobService;
  private final StatsDialog statsDialog;

  @Inject
  public StatsCommand(JobService jobService, StatsDialog statsDialog) {
    this.jobService = jobService;
    this.statsDialog = statsDialog;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("stats")
        // /jobs stats chat <playerName> - admin variant (chat output)
        .then(Commands.literal("chat")
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> source.getSender().hasPermission("jobs.command.admin.stats"))
                .executes(context -> {
                  CommandSourceStack source = context.getSource();
                  CommandSender sender = source.getSender();

                  String playerName = context.getArgument("player", String.class);
                  OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

                  if (target == null) {
                    Mint.sendThemedMessage(sender, "<error>Player not found: " + playerName);
                    return 0;
                  }

                  displayStatsChat(sender, target);
                  return Command.SINGLE_SUCCESS;
                }))
            // /jobs stats chat - player variant (chat output)
            .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                Mint.sendThemedMessage(sender, "<error>This command can only be used by players.");
                return 0;
              }

              displayStatsChat(player, player);
              return Command.SINGLE_SUCCESS;
            }))
        // /jobs stats <playerName> - admin variant (GUI output)
        .then(Commands.argument("player", StringArgumentType.word())
            .requires(source -> source.getSender().hasPermission("jobs.command.admin.stats"))
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              String playerName = context.getArgument("player", String.class);
              OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

              if (target == null) {
                Mint.sendThemedMessage(sender, "<error>Player not found: " + playerName);
                return 0;
              }

              displayStats(sender, target);
              return Command.SINGLE_SUCCESS;
            }))
        // /jobs stats - player variant (GUI output)
        .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          if (!(sender instanceof Player player)) {
            Mint.sendThemedMessage(sender, "<error>This command can only be used by players.");
            return 0;
          }

          displayStats(player, player);
          return Command.SINGLE_SUCCESS;
        });
  }

  /**
   * Displays stats in a GUI dialog. Falls back to chat if sender is not a player.
   */
  private void displayStats(CommandSender viewer, OfflinePlayer target) {
    if (viewer instanceof Player player) {
      displayStatsDialog(player, target);
    } else {
      displayStatsChat(viewer, target);
    }
  }

  /**
   * Displays stats in a GUI dialog.
   */
  private void displayStatsDialog(Player viewer, OfflinePlayer target) {
    List<JobProgression> progressions = jobService.getProgressions(target);
    io.papermc.paper.dialog.Dialog dialog = statsDialog.buildDialog(target, progressions, 1);
    viewer.showDialog(dialog);
  }

  /**
   * Displays stats in chat format (original implementation).
   */
  private void displayStatsChat(CommandSender viewer, OfflinePlayer target) {
    List<JobProgression> progressions = jobService.getProgressions(target);

    String targetName = target.getName() != null ? target.getName() : "Unknown";
    String header = viewer.equals(target)
        ? "<primary>Job Statistics"
        : "<primary>" + targetName + "'s Job Statistics";

    Mint.sendThemedMessage(viewer, "");
    Mint.sendThemedMessage(viewer, "<neutral>━━━━━━━━━ " + header + " <neutral> ━━━━━━━━━");
    Mint.sendThemedMessage(viewer, "");

    if (progressions.isEmpty()) {
      String message = viewer.equals(target)
          ? "<neutral>  You are not in any jobs."
          : "<neutral>  " + targetName + " is not in any jobs.";
      Mint.sendThemedMessage(viewer, message);
      Mint.sendThemedMessage(viewer, "<neutral>  Use <secondary>/jobs join<neutral> to join a job.");
      Mint.sendThemedMessage(viewer, "");
    } else {
      PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

      for (JobProgression progression : progressions) {
        displayJobStats(viewer, progression, serializer);
      }
    }

    Mint.sendThemedMessage(viewer, "");
    Mint.sendThemedMessage(viewer, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Mint.sendThemedMessage(viewer, "");
  }

  private void displayJobStats(CommandSender viewer, JobProgression progression,
                               PlainTextComponentSerializer serializer) {
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
      percentage = xpCurrent.divide(xpTotal, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100))
          .doubleValue();
    }

    // Build hover text with all metadata
    Component hoverText = buildHoverText(viewer, currentLevel, maxLevel, currentXp, xpCurrent, xpTotal, percentage);

    // Build main display: bar + Lvl. [level] + [name]
    String bar = createProgressBar(percentage);
    Component barComponent = Mint.createThemedComponent(viewer, bar);
    Component jobName = progression.job().displayName();
    Component mainDisplay = Component.text("  ")
        .append(barComponent)
        .append(Component.space())
        .append(Mint.createThemedComponent(viewer, "<neutral>Lvl. "))
        .append(Mint.createThemedComponent(viewer, "<secondary>" + currentLevel))
        .append(Component.space())
        .append(jobName)
        .hoverEvent(HoverEvent.showText(hoverText));

    viewer.sendMessage(mainDisplay);
  }

  private Component buildHoverText(CommandSender viewer, int currentLevel, int maxLevel, BigDecimal currentXp,
                                   BigDecimal xpCurrent, BigDecimal xpTotal, double percentage) {
    String percentageStr = String.format("%.1f%%", percentage);
    String xpCurrentStr = formatFullNumber(xpCurrent);
    String xpTotalStr = xpTotal != null ? formatFullNumber(xpTotal) : "MAX";
    String totalXpStr = formatFullNumber(currentXp);
    String progressColor = getProgressColorTag(percentage);

    return Mint.createThemedComponent(viewer,
        "<neutral>Level: <primary>" + currentLevel + " / " + maxLevel +
        "\n<neutral>Progress: <" + progressColor + ">" + percentageStr +
        "\n<neutral>XP in level: <secondary>" + xpCurrentStr + " / " + xpTotalStr +
        "\n<neutral>Total XP: <accent>" + totalXpStr);
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
