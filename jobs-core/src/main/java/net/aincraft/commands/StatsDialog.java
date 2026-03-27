package net.aincraft.commands;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.JobProgression;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;

/**
 * Builds dialog-based displays for job statistics.
 * Provides paginated view of player's job progressions with visual progress bars.
 */
public class StatsDialog {

  private static final String NAMESPACE = "modularjobs";
  private static final int DIALOG_WIDTH = 1000;
  private static final int JOBS_PER_PAGE = 5;

  /**
   * Calculates total pages needed to display all progressions.
   */
  public static int calculateTotalPages(List<JobProgression> progressions) {
    return Math.max(1, (int) Math.ceil((double) progressions.size() / JOBS_PER_PAGE));
  }

  /**
   * Builds a dialog displaying job statistics for the given progressions.
   *
   * @param target The player whose stats are being displayed
   * @param progressions List of job progressions to display
   * @param page Current page number (1-indexed)
   * @return A Dialog instance ready to be shown to the player
   */
  public Dialog buildDialog(OfflinePlayer target, List<JobProgression> progressions, int page) {
    DialogBase dialogBase = buildDialogBase(target, progressions, page);

    int totalPages = calculateTotalPages(progressions);
    List<ActionButton> buttons = new ArrayList<>();

    // Previous button
    if (page > 1) {
      buttons.add(createNavigationButton(
          "Previous",
          TextColor.color(0xAEFFC1),
          "Go to page " + (page - 1),
          Key.key(NAMESPACE, "stats/prev"),
          target,
          page
      ));
    } else {
      buttons.add(createDisabledButton("Previous", "First page"));
    }

    // Page indicator button (disabled, just for display)
    buttons.add(createDisabledButton(
        "Page " + page + "/" + totalPages,
        "Current page"
    ));

    // Next button
    if (page < totalPages) {
      buttons.add(createNavigationButton(
          "Next",
          TextColor.color(0xAEFFC1),
          "Go to page " + (page + 1),
          Key.key(NAMESPACE, "stats/next"),
          target,
          page
      ));
    } else {
      buttons.add(createDisabledButton("Next", "Last page"));
    }

    return Dialog.create(builder -> builder.empty()
        .base(dialogBase)
        .type(DialogType.multiAction(buttons).build())
    );
  }

  private ActionButton createNavigationButton(String label, TextColor color,
      String tooltip, Key actionKey, OfflinePlayer target, int currentPage) {
    BinaryTagHolder data = encodeNavigationData(target, currentPage);
    return ActionButton.builder(Component.text(label, color))
        .tooltip(Component.text(tooltip, TextColor.color(0xAEB4BF)))
        .width(300)
        .action(DialogAction.customClick(actionKey, data))
        .build();
  }

  private ActionButton createDisabledButton(String label, String tooltip) {
    return ActionButton.builder(Component.text(label, TextColor.color(0xAEB4BF)))
        .tooltip(Component.text(tooltip, TextColor.color(0xAEB4BF)))
        .width(300)
        .action(null) // No action = disabled button
        .build();
  }

  private static BinaryTagHolder encodeNavigationData(OfflinePlayer target, int currentPage) {
    // Create SNBT format: {uuid:"player-uuid",page:1}
    String uuid = target.getUniqueId().toString();
    String snbt = String.format("{uuid:\"%s\",page:%d}", uuid, currentPage);
    return BinaryTagHolder.binaryTagHolder(snbt);
  }

  private DialogBase buildDialogBase(OfflinePlayer target, List<JobProgression> progressions, int page) {
    List<DialogBody> bodies = buildDialogBody(target, progressions, page);

    String targetName = target.getName() != null ? target.getName() : "Unknown";
    Component title = Component.text()
        .append(Component.text("Job Statistics", TextColor.color(0xAEB4BF)))
        .build();

    return DialogBase.create(
        title,
        null,
        true,  // canCloseWithEscape
        false, // pause
        DialogBase.DialogAfterAction.CLOSE,
        bodies,
        List.of()
    );
  }

  private List<DialogBody> buildDialogBody(OfflinePlayer target, List<JobProgression> progressions, int page) {
    List<DialogBody> bodies = new ArrayList<>();

    // Header
    bodies.add(buildHeader(target));
    bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));

    if (progressions.isEmpty()) {
      String targetName = target.getName() != null ? target.getName() : "Unknown";
      bodies.add(DialogBody.plainMessage(
          Component.text("No jobs joined yet. Use /jobs join to join a job.", TextColor.color(0xAEB4BF)),
          DIALOG_WIDTH
      ));
    } else {
      // Paginate progressions
      int start = (page - 1) * JOBS_PER_PAGE;
      int end = Math.min(start + JOBS_PER_PAGE, progressions.size());

      for (int i = start; i < end; i++) {
        JobProgression progression = progressions.get(i);
        bodies.addAll(buildJobSection(progression));
        if (i < end - 1) {
          bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));
        }
      }
    }

    bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));

    return bodies;
  }

  private PlainMessageDialogBody buildHeader(OfflinePlayer target) {
    String targetName = target.getName() != null ? target.getName() : "Unknown";
    Component header = Component.text()
        .append(Component.text("━━━━━━ ", TextColor.color(0xAEB4BF)))
        .append(Component.text(targetName + "'s Jobs", TextColor.color(0x3FB3D5)))
        .append(Component.text(" ━━━━━━", TextColor.color(0xAEB4BF)))
        .build();

    return DialogBody.plainMessage(header, DIALOG_WIDTH);
  }

  private List<DialogBody> buildJobSection(JobProgression progression) {
    List<DialogBody> bodies = new ArrayList<>();

    int currentLevel = progression.level();
    BigDecimal currentXp = progression.experience();
    int maxLevel = progression.job().maxLevel();

    // Calculate percentage and XP values
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

    // Job name header
    Component jobHeader = Component.text()
        .append(progression.job().displayName())
        .append(Component.text(" ", TextColor.color(0xAEB4BF)))
        .append(Component.text("[Lvl. " + currentLevel + "/" + maxLevel + "]", TextColor.color(0xFF8600)))
        .build();
    bodies.add(DialogBody.plainMessage(jobHeader, DIALOG_WIDTH));

    // Progress bar
    bodies.add(DialogBody.plainMessage(buildProgressBar(percentage), DIALOG_WIDTH));

    // XP info
    String xpCurrentStr = formatFullNumber(xpCurrent);
    String xpTotalStr = xpTotal != null ? formatFullNumber(xpTotal) : "MAX";
    String totalXpStr = formatFullNumber(currentXp);
    TextColor progressColor = getProgressColor(percentage);

    Component xpInfo = Component.text()
        .append(Component.text("  Progress: ", TextColor.color(0xAEB4BF)))
        .append(Component.text(String.format("%.1f%%", percentage), progressColor))
        .append(Component.text(" | XP: ", TextColor.color(0xAEB4BF)))
        .append(Component.text(xpCurrentStr + "/" + xpTotalStr, TextColor.color(0x3FB3D5)))
        .append(Component.text(" | Total: ", TextColor.color(0xAEB4BF)))
        .append(Component.text(totalXpStr, TextColor.color(0xA1E0E0)))
        .build();
    bodies.add(DialogBody.plainMessage(xpInfo, DIALOG_WIDTH));

    return bodies;
  }

  private Component buildProgressBar(double percentage) {
    int barLength = 40;
    int filled = (int) Math.round(percentage / 100.0 * barLength);
    filled = Math.min(barLength, Math.max(0, filled));

    TextColor progressColor = getProgressColor(percentage);
    TextColor emptyColor = TextColor.color(0x444444);

    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < barLength; i++) {
      if (i < filled) {
        bar.append("|");
      } else {
        bar.append(".");
      }
    }

    return Component.text()
        .append(Component.text("  [", TextColor.color(0xAEB4BF)))
        .append(Component.text(bar.toString(), progressColor))
        .append(Component.text("]", TextColor.color(0xAEB4BF)))
        .build();
  }

  private TextColor getProgressColor(double percentage) {
    if (percentage >= 75) {
      return TextColor.color(0x55FFFF); // Cyan (accent)
    } else if (percentage >= 50) {
      return TextColor.color(0xFFFF55); // Yellow (secondary)
    } else if (percentage >= 25) {
      return TextColor.color(0xFF8600); // Gold (primary)
    } else {
      return TextColor.color(0xFF5555); // Red (error)
    }
  }

  private String formatFullNumber(BigDecimal number) {
    return String.format("%,d", number.setScale(0, RoundingMode.HALF_UP).intValue());
  }
}
