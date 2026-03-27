package net.aincraft.commands.info;

import dev.mintychochip.mint.Mint;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.commands.Page;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Chat-based implementation for displaying job info with pagination.
 * Uses clickable components for navigation as an alternative to the GUI dialog.
 */
public final class ChatInfoPageConsumer {

  private static final int ENTRIES_PER_PAGE = 5;

  /**
   * Displays job information in chat with clickable pagination.
   *
   * @param job The job to display info for
   * @param tasks Map of action types to their tasks
   * @param sender The command sender (must be a player for clickable navigation)
   * @param page The current page number (1-indexed)
   */
  public void consume(Job job, Map<ActionType, List<JobTask>> tasks, CommandSender sender,
      int page) {
    String jobName = job.key().value();
    int totalPages = calculateTotalPages(tasks);

    // Validate page
    if (page < 1 || page > totalPages) {
      Mint.sendThemedMessage(sender, "<error>Invalid page. Valid: 1-" + totalPages);
      return;
    }

    // Header
    Mint.sendThemedMessage(sender, "");
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━ <primary>Job Info </primary><accent>"
        + PlainTextComponentSerializer.plainText().serialize(job.displayName())
        + "</accent> <neutral>━━━━━━");

    // Job description and max level
    Mint.sendThemedMessage(sender, "");
    sender.sendMessage(job.description().color(TextColor.color(0xAEB4BF)));
    Mint.sendThemedMessage(sender, "<neutral>Max Level: <accent>" + job.maxLevel());
    Mint.sendThemedMessage(sender, "");
    Mint.sendThemedMessage(sender, "<neutral>Page " + page + " of " + totalPages);
    Mint.sendThemedMessage(sender, "");

    // Paginate action types
    List<Map.Entry<ActionType, List<JobTask>>> entries = tasks.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .collect(Collectors.toList());

    int start = (page - 1) * ENTRIES_PER_PAGE;
    int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

    // Display action types and their tasks
    for (int i = start; i < end; i++) {
      Map.Entry<ActionType, List<JobTask>> entry = entries.get(i);
      displayActionTypeSection(entry.getKey(), entry.getValue(), sender);
    }

    // Navigation footer
    Mint.sendThemedMessage(sender, "");
    Component navigation = buildNavigation(jobName, page, totalPages);
    sender.sendMessage(navigation);
    Mint.sendThemedMessage(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Mint.sendThemedMessage(sender, "");
  }

  /**
   * Calculates the total number of pages needed to display all action types.
   */
  public static int calculateTotalPages(Map<ActionType, List<JobTask>> tasks) {
    int nonEmptyActionTypes = (int) tasks.values().stream()
        .filter(list -> !list.isEmpty())
        .count();
    return Math.max(1, (int) Math.ceil((double) nonEmptyActionTypes / ENTRIES_PER_PAGE));
  }

  /**
   * Displays a section for a single action type with its tasks.
   */
  private void displayActionTypeSection(ActionType type, List<JobTask> tasks,
      CommandSender sender) {
    // Action type header
    Mint.sendThemedMessage(sender, "<neutral>━━ <accent>" + formatActionTypeName(type.name())
        + "</accent> <neutral>━━");

    // Show tasks
    for (JobTask task : tasks) {
      Component taskEntry = buildTaskEntry(task);
      sender.sendMessage(taskEntry);
    }
  }

  /**
   * Builds a component for a single task entry.
   */
  private Component buildTaskEntry(JobTask task) {
    return Component.text()
        .append(Component.text("  ● ", TextColor.color(0xAEB4BF)))
        .append(Component.text(formatContextKey(task.contextKey()), TextColor.color(0xA1E0E0)))
        .append(Component.text(" → ", TextColor.color(0xAEB4BF)))
        .append(buildPayableComponent(task.payables()))
        .build();
  }

  /**
   * Builds a component for displaying payables.
   */
  private Component buildPayableComponent(List<Payable> payables) {
    if (payables.isEmpty()) {
      return Component.text("No rewards", TextColor.color(0xAEB4BF));
    }

    Component result = Component.empty();
    for (int i = 0; i < payables.size(); i++) {
      result = result.append(payables.get(i).asComponent());
      if (i < payables.size() - 1) {
        result = result.append(Component.text(", ", TextColor.color(0xAEB4BF)));
      }
    }
    return result;
  }

  /**
   * Builds navigation buttons with clickable components.
   */
  private Component buildNavigation(String jobName, int currentPage, int maxPages) {
    Component nav = Component.empty();

    // Previous button
    if (currentPage > 1) {
      Component prevButton = Component.text("[< Previous]")
          .color(NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs info " + jobName + " " + (currentPage - 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage - 1)));
      nav = nav.append(prevButton);
    } else {
      Component prevButton = Component.text("[< Previous]")
          .color(NamedTextColor.DARK_GRAY);
      nav = nav.append(prevButton);
    }

    nav = nav.append(Component.text("  ", NamedTextColor.GRAY));

    // Next button
    if (currentPage < maxPages) {
      Component nextButton = Component.text("[Next >]")
          .color(NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand("/jobs info " + jobName + " " + (currentPage + 1)))
          .hoverEvent(Component.text("Click to go to page " + (currentPage + 1)));
      nav = nav.append(nextButton);
    } else {
      Component nextButton = Component.text("[Next >]")
          .color(NamedTextColor.DARK_GRAY);
      nav = nav.append(nextButton);
    }

    return nav;
  }

  /**
   * Formats an action type enum name to a readable string.
   * E.g., "BREAK_BLOCK" -> "Break Block"
   */
  private static String formatActionTypeName(String name) {
    return Arrays.stream(name.toLowerCase().split("_"))
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  /**
   * Formats a context key to a readable string.
   * E.g., "stone" -> "Stone" or "deepslate_ore" -> "Deepslate Ore"
   */
  private static String formatContextKey(Key key) {
    String value = key.value();
    return Arrays.stream(value.split("[_/]"))
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }
}
