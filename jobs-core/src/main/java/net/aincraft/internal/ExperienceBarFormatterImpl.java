package net.aincraft.internal;

import java.math.BigDecimal;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

final class ExperienceBarFormatterImpl implements ExperienceBarFormatter {

  @NotNull
  private NameFormatter formatter = new NameFormatterImpl();

  private Color color = Color.BLUE;

  private Overlay overlay = Overlay.PROGRESS;

  @Override
  public BossBar format(@NotNull BossBar bossBar,
      @NotNull ExperiencePayableHandler.ExperienceBarContext context) {
    return bossBar.name(formatter.format(context))
        .progress(progress(context.progression()))
        .color(color)
        .overlay(overlay);
  }

  @Override
  public void setColor(@NotNull Color color) {
    this.color = color;
  }

  @Override
  public void setOverlay(@NotNull Overlay overlay) {
    this.overlay = overlay;
  }

  @Override
  public void setNameFormatter(@NotNull NameFormatter formatter) throws IllegalArgumentException {
    this.formatter = formatter;
  }

  @Internal
  private static float progress(JobProgressionView progression) {
    int currentLevel = progression.getLevel();
    double currentLevelExpRequired = progression.getExperienceForLevel(currentLevel);
    double nextLevelExpRequired = progression.getExperienceForLevel(currentLevel + 1);
    double needed = nextLevelExpRequired - currentLevelExpRequired;
    if (needed <= 0.0) {
      return 1.0f;
    }
    return Math.min(1.0f,
        (float) (progression.getExperience().doubleValue() - currentLevelExpRequired)
            / (float) needed);
  }

  private static final class NameFormatterImpl implements NameFormatter {

    private static final String FORMAT = "Lvl. <level> <job-name>: <xp>/<total-xp> xp (<payable>)";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public @NotNull Component format(
        @NotNull ExperiencePayableHandler.ExperienceBarContext context) {
      JobProgressionView progression = context.progression();
      int level = progression.getLevel();
      double experienceForNext = progression.getExperienceForLevel(level + 1);
      return MINI_MESSAGE.deserialize(FORMAT, TagResolver.builder()
          .tag("level", Tag.inserting(Component.text(level)))
          .tag("job-name", Tag.inserting(progression.getJob().getDisplayName()))
          .tag("xp", Tag.inserting(Component.text(
              progression.getExperience().doubleValue())))
          .tag("total-xp", Tag.inserting(Component.text(experienceForNext)))
          .tag("payable", Tag.inserting(payableComponent(context.amount())))
          .build());
    }

    private static Component payableComponent(BigDecimal amount) {
      double value = amount.doubleValue();
      Component component = Component.empty();
      if (value > 0.0) {
        component = component.append(Component.text("+"));
      }
      if (value < 0.0) {
        component = component.append(Component.text("-"));
      }
      return component.append(Component.text(value));
    }
  }
}
