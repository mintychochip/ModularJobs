package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import dev.mintychochip.mint.Mint;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";
  private static final int ACTION_TYPES_PER_PAGE = 15;
  private static final int DIALOG_WIDTH = 1000;

  @Inject
  public InfoCommand(JobService jobService, JobResolver jobResolver) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("info")
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobResolver.getPlainNames().forEach(builder::suggest);
          return builder.buildFuture();
        })
            .executes(context -> {
              // Default to page 1
              return executeCommand(context.getSource(),
                  context.getArgument("job", String.class), 1);
            })
            .then(Commands.argument("pageNumber", IntegerArgumentType.integer(1))
                .executes(context -> {
                  return executeCommand(context.getSource(),
                      context.getArgument("job", String.class),
                      IntegerArgumentType.getInteger(context, "pageNumber"));
                })
            )
        );
  }

  private int executeCommand(CommandSourceStack source, String jobName, int page) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Mint.sendMessage(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Mint.sendMessage(sender, "<error>The job you specified does not exist.");
      return 0;
    }

    Map<ActionType, List<JobTask>> tasks = jobService.getAllTasks(job);
    int totalPages = calculateTotalPages(tasks);

    if (page < 1 || page > totalPages) {
      Mint.sendMessage(player, "<error>Invalid page. Valid: 1-" + totalPages);
      return 0;
    }

    Dialog dialog = buildDialog(job, tasks, page);
    player.showDialog(dialog);

    return Command.SINGLE_SUCCESS;
  }

  public static int calculateTotalPages(Map<ActionType, List<JobTask>> tasks) {
    return Math.max(1, (int) Math.ceil((double) tasks.size() / ACTION_TYPES_PER_PAGE));
  }

  public Dialog buildDialog(Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    DialogBase dialogBase = buildDialogBase(job, tasks, page);

    int totalPages = calculateTotalPages(tasks);
    List<ActionButton> buttons = new ArrayList<>();

    // Previous button
    if (page > 1) {
      buttons.add(createNavigationButton(
          "◀ Previous",
          TextColor.color(0xAEFFC1),
          "Go to page " + (page - 1),
          Key.key(DEFAULT_NAMESPACE, "info/prev"),
          job.key().value(),
          page
      ));
    } else {
      buttons.add(createDisabledButton("◀ Previous", "First page"));
    }

    // Page indicator button (disabled, just for display)
    buttons.add(createDisabledButton(
        "Page " + page + "/" + totalPages,
        "Current page"
    ));

    // Next button
    if (page < totalPages) {
      buttons.add(createNavigationButton(
          "Next ▶",
          TextColor.color(0xAEFFC1),
          "Go to page " + (page + 1),
          Key.key(DEFAULT_NAMESPACE, "info/next"),
          job.key().value(),
          page
      ));
    } else {
      buttons.add(createDisabledButton("Next ▶", "Last page"));
    }

    return Dialog.create(builder -> builder.empty()
        .base(dialogBase)
        .type(DialogType.multiAction(buttons).build())
    );
  }

  private ActionButton createNavigationButton(String label, TextColor color,
      String tooltip, Key actionKey, String jobName, int currentPage) {
    BinaryTagHolder data = encodeNavigationData(jobName, currentPage);
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

  private static BinaryTagHolder encodeNavigationData(String jobName, int currentPage) {
    // Create SNBT format: {jobName:"miner",page:1}
    String snbt = String.format("{jobName:\"%s\",page:%d}", jobName, currentPage);
    return BinaryTagHolder.binaryTagHolder(snbt);
  }

  private DialogBase buildDialogBase(Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    List<DialogBody> bodies = buildDialogBody(job, tasks, page);

    return DialogBase.create(
        Component.text()
            .append(Component.text("Job Info: ", TextColor.color(0xAEB4BF)))
            .append(job.displayName())
            .build(),
        null,
        true,  // canCloseWithEscape
        false, // pause
        DialogBase.DialogAfterAction.CLOSE,
        bodies,
        List.of()
    );
  }

  private List<DialogBody> buildDialogBody(Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    List<DialogBody> bodies = new ArrayList<>();

    // Header
    bodies.add(buildJobHeader(job));
    bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));

    // Paginate action types
    List<Map.Entry<ActionType, List<JobTask>>> entries = new ArrayList<>(tasks.entrySet());
    int start = (page - 1) * ACTION_TYPES_PER_PAGE;
    int end = Math.min(start + ACTION_TYPES_PER_PAGE, entries.size());

    for (int i = start; i < end; i++) {
      var entry = entries.get(i);
      if (!entry.getValue().isEmpty()) {
        bodies.addAll(buildActionTypeSection(entry.getKey(), entry.getValue()));
        bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));
      }
    }

    return bodies;
  }

  private PlainMessageDialogBody buildJobHeader(Job job) {
    Component header = Component.text()
        .append(job.displayName())
        .append(Component.newline())
        .append(job.description().color(TextColor.color(0xAEB4BF)))
        .append(Component.newline())
        .append(Component.text("Max Level: ", TextColor.color(0xAEB4BF)))
        .append(Component.text(job.maxLevel(), TextColor.color(0xFF8600)))
        .build();

    return DialogBody.plainMessage(header, DIALOG_WIDTH);
  }

  private List<DialogBody> buildActionTypeSection(ActionType type, List<JobTask> tasks) {
    List<DialogBody> bodies = new ArrayList<>();

    Component header = Component.text()
        .append(Component.text("━━ ", TextColor.color(0xAEB4BF)))
        .append(Component.text(formatActionTypeName(type.name()), TextColor.color(0x3FB3D5)))
        .append(Component.text(" ━━", TextColor.color(0xAEB4BF)))
        .build();
    bodies.add(DialogBody.plainMessage(header, DIALOG_WIDTH));

    // Show all tasks (no limit since we have more space now)
    for (JobTask task : tasks) {
      bodies.add(buildTaskEntry(task));
    }

    return bodies;
  }

  private DialogBody buildTaskEntry(JobTask task) {
    return DialogBody.plainMessage(
        Component.text()
            .append(Component.text("  ● ", TextColor.color(0xAEB4BF)))
            .append(Component.text(formatContextKey(task.contextKey()), TextColor.color(0xA1E0E0)))
            .append(Component.text(" → ", TextColor.color(0xAEB4BF)))
            .append(buildPayableComponent(task.payables()))
            .build(),
        DIALOG_WIDTH
    );
  }

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

  private static String formatActionTypeName(String name) {
    return Arrays.stream(name.toLowerCase().split("_"))
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  private static String formatContextKey(Key key) {
    String value = key.value();
    return Arrays.stream(value.split("[_/]"))
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }
}
