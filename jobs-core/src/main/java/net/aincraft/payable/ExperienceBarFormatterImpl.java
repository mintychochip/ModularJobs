package net.aincraft.payable;

import com.google.inject.Inject;
import java.math.BigDecimal;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ExperiencePayableHandler;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

final class ExperienceBarFormatterImpl implements ExperienceBarFormatter {

  private final ExperienceBarColorProvider colorProvider;

  private Overlay overlay = Overlay.PROGRESS;

  private final NameFormatter formatter = new NameFormatter();

  @Inject
  ExperienceBarFormatterImpl(ExperienceBarColorProvider colorProvider) {
    this.colorProvider = colorProvider;
  }

  @Override
  public BossBar format(@NotNull BossBar bossBar,
      @NotNull ExperiencePayableHandler.ExperienceBarContext context) {
    return bossBar.name(formatter.format(context))
        .progress(progress(context.progression()))
        .color(colorProvider.getColor(context.player()))
        .overlay(overlay);
  }

  @Override
  public void setOverlay(@NotNull Overlay overlay) {
    this.overlay = overlay;
  }

  @Internal
  private static float progress(JobProgressionView progression) {
    int level = progression.level();

    double currentRequired = progression.experienceForLevel(level).doubleValue();
    double nextRequired = progression.experienceForLevel(level + 1).doubleValue();
    double needed = nextRequired - currentRequired;

    if (needed <= 0.0) {
      return 1.0f;
    }

    double xp = progression.experience().doubleValue();
    double ratio = (xp - currentRequired) / needed;

    if (ratio < 0.0) {
      return 0.0f;
    }
    return Math.min((float) ratio, 1.0f);
  }


  private static final class NameFormatter {

    private static final String FORMAT = "Lvl. <level> <job-name>: <xp>/<total-xp> xp (<payable>)";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public @NotNull Component format(
        @NotNull ExperiencePayableHandler.ExperienceBarContext context) {
      JobProgressionView progression = context.progression();
      int level = progression.level();
      BigDecimal experienceForNext = progression.experienceForLevel(level + 1);
      return MINI_MESSAGE.deserialize(FORMAT, TagResolver.builder()
          .tag("level", Tag.inserting(Component.text(level)))
          .tag("job-name",
              Tag.inserting(Component.empty().append(progression.job().displayName())))
          .tag("xp", Tag.inserting(Component.text(
              progression.experience().doubleValue())))
          .tag("total-xp", Tag.inserting(Component.text(experienceForNext.doubleValue())))
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
