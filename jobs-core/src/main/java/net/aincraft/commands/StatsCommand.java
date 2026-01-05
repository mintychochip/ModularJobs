package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements JobsCommand {

  private final JobService jobService;

  @Inject
  public StatsCommand(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("stats")
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
            return 0;
          }

          List<JobProgression> progressions = jobService.getProgressions(player);

          // Header
          player.sendMessage(Component.text("━━━━━━━━━ ", NamedTextColor.GRAY)
              .append(Component.text("Job Statistics", NamedTextColor.GOLD))
              .append(Component.text(" ━━━━━━━━━", NamedTextColor.GRAY)));
          player.sendMessage(Component.empty());

          if (progressions.isEmpty()) {
            player.sendMessage(Component.text("  You are not in any jobs.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Use ", NamedTextColor.GRAY)
                .append(Component.text("/jobs join", NamedTextColor.YELLOW))
                .append(Component.text(" to join a job.", NamedTextColor.GRAY)));
            player.sendMessage(Component.empty());
          } else {
            for (JobProgression progression : progressions) {
              displayJobStats(player, progression);
            }
          }

          // Footer
          player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));

          return Command.SINGLE_SUCCESS;
        });
  }

  private void displayJobStats(Player player, JobProgression progression) {
    int currentLevel = progression.level();
    BigDecimal currentXp = progression.experience();
    int maxLevel = progression.job().maxLevel();

    // Build hover metadata component
    Component hoverInfo = buildHoverMetadata(progression, currentLevel, currentXp, maxLevel);

    // Calculate percentage and XP values for display
    double percentage;
    String xpCurrent;
    String xpTotal;

    if (currentLevel >= maxLevel) {
      percentage = 100.0;
      xpCurrent = formatNumber(currentXp);
      xpTotal = "MAX";
    } else {
      BigDecimal currentLevelXp = progression.experienceForLevel(currentLevel);
      BigDecimal nextLevelXp = progression.experienceForLevel(currentLevel + 1);
      BigDecimal xpIntoLevel = currentXp.subtract(currentLevelXp);
      BigDecimal xpNeeded = nextLevelXp.subtract(currentLevelXp);
      percentage = xpIntoLevel.divide(xpNeeded, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100))
          .doubleValue();
      xpCurrent = formatNumber(xpIntoLevel);
      xpTotal = formatNumber(xpNeeded);
    }

    // Job name on first line
    Component jobNameLine = Component.text("  ")
        .append(progression.job().displayName());
    player.sendMessage(jobNameLine);

    // Progress bar on second line (indented) - all bars align this way
    Component barLine = Component.text("    ")
        .append(createProgressBar(percentage))
        .append(Component.text(" ", NamedTextColor.GRAY))
        .append(Component.text(String.format("%.1f%%", percentage), NamedTextColor.YELLOW))
        .append(Component.text(" (", NamedTextColor.GRAY))
        .append(Component.text(xpCurrent, NamedTextColor.AQUA))
        .append(Component.text("/", NamedTextColor.DARK_GRAY))
        .append(Component.text(xpTotal, NamedTextColor.AQUA))
        .append(Component.text(")", NamedTextColor.GRAY))
        .hoverEvent(hoverInfo);
    player.sendMessage(barLine);
  }

  private Component buildHoverMetadata(JobProgression progression, int currentLevel,
                                       BigDecimal currentXp, int maxLevel) {
    Component hover = Component.text()
        .append(Component.text("Level: ", NamedTextColor.GRAY))
        .append(Component.text(currentLevel, NamedTextColor.GREEN))
        .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
        .append(Component.text(maxLevel, NamedTextColor.GREEN))
        .append(Component.newline())
        .build();

    if (currentLevel >= maxLevel) {
      hover = hover.append(Component.text("XP: ", NamedTextColor.GRAY))
          .append(Component.text(formatNumber(currentXp), NamedTextColor.AQUA))
          .append(Component.text(" (MAX)", NamedTextColor.GOLD));
    } else {
      BigDecimal currentLevelXp = progression.experienceForLevel(currentLevel);
      BigDecimal nextLevelXp = progression.experienceForLevel(currentLevel + 1);
      BigDecimal xpIntoLevel = currentXp.subtract(currentLevelXp);
      BigDecimal xpNeeded = nextLevelXp.subtract(currentLevelXp);
      BigDecimal xpRemaining = nextLevelXp.subtract(currentXp);

      double percentage = xpIntoLevel.divide(xpNeeded, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100))
          .doubleValue();

      hover = hover
          .append(Component.text("XP: ", NamedTextColor.GRAY))
          .append(Component.text(formatNumber(xpIntoLevel), NamedTextColor.AQUA))
          .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
          .append(Component.text(formatNumber(xpNeeded), NamedTextColor.AQUA))
          .append(Component.text(String.format(" (%.1f%%)", percentage), NamedTextColor.YELLOW))
          .append(Component.newline())
          .append(Component.text("Next Level: ", NamedTextColor.GRAY))
          .append(Component.text(formatNumber(xpRemaining), NamedTextColor.GREEN))
          .append(Component.text(" XP", NamedTextColor.GRAY));
    }

    return hover;
  }

  private Component createProgressBar(double percentage) {
    int barLength = 30;
    int filled = (int) Math.round(percentage / 100.0 * barLength);
    filled = Math.min(barLength, Math.max(0, filled));

    Component bar = Component.text("[", NamedTextColor.GRAY);

    for (int i = 0; i < barLength; i++) {
      if (i < filled) {
        // Color gradient based on percentage
        NamedTextColor color = getProgressColor(percentage);
        bar = bar.append(Component.text("|", color));
      } else {
        bar = bar.append(Component.text("|", NamedTextColor.DARK_GRAY));
      }
    }

    bar = bar.append(Component.text("]", NamedTextColor.GRAY));
    return bar;
  }

  private NamedTextColor getProgressColor(double percentage) {
    if (percentage >= 75) {
      return NamedTextColor.GREEN;
    } else if (percentage >= 50) {
      return NamedTextColor.YELLOW;
    } else if (percentage >= 25) {
      return NamedTextColor.GOLD;
    } else {
      return NamedTextColor.RED;
    }
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
