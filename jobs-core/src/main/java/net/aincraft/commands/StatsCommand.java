package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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
          Mint.sendMessage(player, "");
          Mint.sendMessage(player, "<neutral>━━━━━━━━━ <primary>Job Statistics<neutral> ━━━━━━━━━");
          Mint.sendMessage(player, "");

          if (progressions.isEmpty()) {
            Mint.sendMessage(player, "<neutral>  You are not in any jobs.");
            Mint.sendMessage(player, "<neutral>  Use <secondary>/jobs join<neutral> to join a job.");
            Mint.sendMessage(player, "");
          } else {
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

            for (JobProgression progression : progressions) {
              displayJobStats(player, progression, serializer);
            }
          }

          // Footer
          Mint.sendMessage(player, "");
          Mint.sendMessage(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
          Mint.sendMessage(player, "");

          return Command.SINGLE_SUCCESS;
        });
  }

  private void displayJobStats(Player player, JobProgression progression,
                               PlainTextComponentSerializer serializer) {
    int currentLevel = progression.level();
    BigDecimal currentXp = progression.experience();
    int maxLevel = progression.job().maxLevel();

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

    // Build progress bar message with Mint tags
    String bar = createProgressBar(percentage);
    String percentageStr = String.format("%.1f%%", percentage);
    String jobNamePlain = serializer.serialize(progression.job().displayName());

    // Extract job color from displayName
    String jobColorTag = extractJobColorTag(progression.job().displayName());

    String message = "  " + bar + " <accent>" + percentageStr + " <neutral>(<secondary>" +
        xpCurrent + "<neutral>/<secondary>" + xpTotal + "<neutral>) <" + jobColorTag + ">" + jobNamePlain;

    Mint.sendMessage(player, message);
  }


  private String createProgressBar(double percentage) {
    int barLength = 30;
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

  private String extractJobColorTag(Component displayName) {
    // Try to extract color from the Component
    if (displayName.color() != null) {
      return displayName.color().asHexString();
    }

    // Fallback: serialize and extract from MiniMessage tags
    GsonComponentSerializer gson = GsonComponentSerializer.gson();
    String json = gson.serialize(displayName);

    // Extract hex color from JSON if present
    Pattern hexPattern = Pattern.compile("\"color\":\"(#[0-9A-Fa-f]{6})\"");
    Matcher matcher = hexPattern.matcher(json);
    if (matcher.find()) {
      return matcher.group(1);
    }

    return "#FFD700"; // Fallback to gold if no color found
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
