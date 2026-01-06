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
import net.aincraft.config.ColorScheme;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.mintychochip.mint.Mint;

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
            Mint.sendMessage(sender, "<error>This command can only be used by players.");
            return 0;
          }

          List<JobProgression> progressions = jobService.getProgressions(player);

          // Header
          Mint.sendMessage(player, "<neutral>━━━━━━━━━ <primary>Job Statistics<neutral> ━━━━━━━━━");
          player.sendMessage(Component.empty());

          if (progressions.isEmpty()) {
            Mint.sendMessage(player, "<neutral>  You are not in any jobs.");
            Mint.sendMessage(player, "<neutral>  Use <secondary>/jobs join<neutral> to join a job.");
            player.sendMessage(Component.empty());
          } else {
            // Calculate max job name length for alignment
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            int maxNameLength = progressions.stream()
                .map(p -> serializer.serialize(p.job().displayName()).length())
                .max(Integer::compareTo)
                .orElse(0);

            for (JobProgression progression : progressions) {
              displayJobStats(player, progression, maxNameLength, serializer);
            }
          }

          // Footer
          Mint.sendMessage(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");

          return Command.SINGLE_SUCCESS;
        });
  }

  private void displayJobStats(Player player, JobProgression progression, int maxNameLength,
                               PlainTextComponentSerializer serializer) {
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

    // Bar on LEFT, job name on RIGHT - bars naturally align
    Component line = Component.text("  ")
        .append(createProgressBar(percentage))
        .append(Component.text(" "))
        .append(Component.text(String.format("%.1f%%", percentage)).color(TextColor.fromHexString("#FFFF00")))
        .append(Component.text(" ("))
        .append(Component.text(xpCurrent).color(TextColor.fromHexString("#00FFFF")))
        .append(Component.text("/"))
        .append(Component.text(xpTotal).color(TextColor.fromHexString("#00FFFF")))
        .append(Component.text(") "))
        .append(progression.job().displayName())
        .hoverEvent(hoverInfo);

    player.sendMessage(line);
  }

  private Component buildHoverMetadata(JobProgression progression, int currentLevel,
                                       BigDecimal currentXp, int maxLevel) {
    Component hover = Component.text()
        .append(Component.text("Level: ", TextColor.fromHexString("#808080")))
        .append(Component.text(currentLevel, TextColor.fromHexString("#00FFFF")))
        .append(Component.text(" / ", TextColor.fromHexString("#808080")))
        .append(Component.text(maxLevel, TextColor.fromHexString("#00FFFF")))
        .append(Component.newline())
        .build();

    if (currentLevel >= maxLevel) {
      hover = hover.append(Component.text("XP: ", TextColor.fromHexString("#808080")))
          .append(Component.text(formatNumber(currentXp), TextColor.fromHexString("#00FFFF")))
          .append(Component.text(" (MAX)", TextColor.fromHexString("#FFD700")));
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
          .append(Component.text("XP: ", TextColor.fromHexString("#808080")))
          .append(Component.text(formatNumber(xpIntoLevel), TextColor.fromHexString("#00FFFF")))
          .append(Component.text(" / ", TextColor.fromHexString("#808080")))
          .append(Component.text(formatNumber(xpNeeded), TextColor.fromHexString("#00FFFF")))
          .append(Component.text(String.format(" (%.1f%%)", percentage), TextColor.fromHexString("#FFFF00")))
          .append(Component.newline())
          .append(Component.text("Next Level: ", TextColor.fromHexString("#808080")))
          .append(Component.text(formatNumber(xpRemaining), TextColor.fromHexString("#00FFFF")))
          .append(Component.text(" XP", TextColor.fromHexString("#808080")));
    }

    return hover;
  }

  private Component createProgressBar(double percentage) {
    int barLength = 20;
    int filled = (int) Math.round(percentage / 100.0 * barLength);
    filled = Math.min(barLength, Math.max(0, filled));

    Component bar = Component.text("[", TextColor.fromHexString("#808080"));

    for (int i = 0; i < barLength; i++) {
      if (i < filled) {
        // Color gradient based on percentage
        TextColor color = getProgressColor(percentage);
        bar = bar.append(Component.text("|", color));
      } else {
        bar = bar.append(Component.text("|", TextColor.fromHexString("#808080")));
      }
    }

    bar = bar.append(Component.text("]", TextColor.fromHexString("#808080")));
    return bar;
  }

  private TextColor getProgressColor(double percentage) {
    if (percentage >= 75) {
      return TextColor.fromHexString("#00FFFF"); // accent
    } else if (percentage >= 50) {
      return TextColor.fromHexString("#FFFF00"); // secondary
    } else if (percentage >= 25) {
      return TextColor.fromHexString("#FFD700"); // primary
    } else {
      return TextColor.fromHexString("#FF0000"); // error
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
