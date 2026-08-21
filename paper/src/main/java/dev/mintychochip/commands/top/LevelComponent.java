package dev.mintychochip.commands.top;

import dev.mintychochip.JobProgression;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a player's level as a component whose hover text details the current level, experience,
 * and progress toward (or cap at) the job's max level.
 */
final class LevelComponent implements ComponentLike {

  private final JobProgression progression;

  LevelComponent(JobProgression progression) {
    this.progression = progression;
  }

  static LevelComponent of(JobProgression progression) {
    return new LevelComponent(progression);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Computes {@code XP / next-level XP} progress, marking the level as {@code MAX} when the
   * progression has reached the job's maximum level.
   */
  @Override
  public @NotNull Component asComponent() {
    int level = progression.level();
    BigDecimal experience = progression.experience();
    int maxLevel = progression.job().maxLevel();

    Component hover;
    if (level >= maxLevel) {
      // Player is at max level
      hover =
          Component.text()
              .append(Component.text("Level: ", NamedTextColor.GRAY))
              .append(Component.text(level + " (MAX)", NamedTextColor.GOLD))
              .appendNewline()
              .append(Component.text("Experience: ", NamedTextColor.GRAY))
              .append(Component.text(experience.toPlainString(), NamedTextColor.WHITE))
              .build();
    } else {
      // Calculate progress to next level
      BigDecimal currentLevelXp = progression.experienceForLevel(level);
      BigDecimal nextLevelXp = progression.experienceForLevel(level + 1);
      BigDecimal progressXp = experience.subtract(currentLevelXp);
      BigDecimal neededXp = nextLevelXp.subtract(currentLevelXp);

      // Calculate percentage
      double percentage =
          progressXp
              .divide(neededXp, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue();

      hover =
          Component.text()
              .append(Component.text("Level: ", NamedTextColor.GRAY))
              .append(Component.text(level, NamedTextColor.YELLOW))
              .appendNewline()
              .append(Component.text("Progress: ", NamedTextColor.GRAY))
              .append(Component.text(String.format("%.1f%%", percentage), NamedTextColor.GREEN))
              .appendNewline()
              .append(Component.text("XP: ", NamedTextColor.GRAY))
              .append(Component.text(progressXp.toPlainString(), NamedTextColor.WHITE))
              .append(Component.text(" / ", NamedTextColor.GRAY))
              .append(Component.text(neededXp.toPlainString(), NamedTextColor.WHITE))
              .build();
    }

    return Component.text(level).hoverEvent(HoverEvent.showText(hover));
  }
}
