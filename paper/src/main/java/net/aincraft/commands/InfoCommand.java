package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.domain.JobResolver;
import net.aincraft.gui.JobInfoGui;
import net.aincraft.service.JobService;
import net.aincraft.service.PreferencesService;
import net.aincraft.util.Messages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /jobs info — chat listing or craftux inventory GUI (preference-driven).
 *
 * <p>GUI mode uses {@link JobInfoGui}; Paper Dialog is not used on this path.
 */
public class InfoCommand implements JobsCommand {

  private final JobService jobService;
  private final JobResolver jobResolver;
  private final PreferencesService preferencesService;
  private final JobInfoGui jobInfoGui;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  public InfoCommand(
      JobService jobService,
      JobResolver jobResolver,
      PreferencesService preferencesService,
      JobInfoGui jobInfoGui) {
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.preferencesService = preferencesService;
    this.jobInfoGui = jobInfoGui;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("info")
        .then(Commands.literal("chat")
            .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
                .executes(context -> executeChatCommand(context.getSource(),
                    context.getArgument("job", String.class), 1))
                .then(Commands.argument("pageNumber", IntegerArgumentType.integer(1))
                    .executes(context -> executeChatCommand(context.getSource(),
                        context.getArgument("job", String.class),
                        IntegerArgumentType.getInteger(context, "pageNumber"))))))
        .then(Commands.literal("gui")
            .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
                .executes(context -> executeGuiCommand(context.getSource(),
                    context.getArgument("job", String.class), 1))
                .then(Commands.argument("pageNumber", IntegerArgumentType.integer(1))
                    .executes(context -> executeGuiCommand(context.getSource(),
                        context.getArgument("job", String.class),
                        IntegerArgumentType.getInteger(context, "pageNumber"))))))
        .then(Commands.literal("preference")
            .then(Commands.literal("entries")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                    .executes(context -> setEntriesPreference(context.getSource(),
                        IntegerArgumentType.getInteger(context, "count")))))
            .then(Commands.literal("gui")
                .executes(context -> setGuiModePreference(context.getSource(), true)))
            .then(Commands.literal("chat")
                .executes(context -> setGuiModePreference(context.getSource(), false))))
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobResolver.getPlainNames().forEach(builder::suggest);
          return builder.buildFuture();
        })
            .executes(context -> executeCommand(context.getSource(),
                context.getArgument("job", String.class), 1))
            .then(Commands.argument("pageNumber", IntegerArgumentType.integer(1))
                .executes(context -> executeCommand(context.getSource(),
                    context.getArgument("job", String.class),
                    IntegerArgumentType.getInteger(context, "pageNumber")))));
  }

  private int executeCommand(CommandSourceStack source, String jobName, int page) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }
    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>The job you specified does not exist.");
      return 0;
    }
    Map<ActionType, List<JobTask>> tasks = jobService.getAllTasks(job);
    if (preferencesService.prefersGuiMode(player.getUniqueId())) {
      return executeGuiCommandInternal(player, job, tasks, page);
    }
    return executeChatCommandInternal(player, job, tasks, page);
  }

  private int executeChatCommand(CommandSourceStack source, String jobName, int page) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }
    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>The job you specified does not exist.");
      return 0;
    }
    return executeChatCommandInternal(player, job, jobService.getAllTasks(job), page);
  }

  private int executeChatCommandInternal(
      Player player, Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    int entriesPerPage = preferencesService.getEntriesPerPage(player.getUniqueId());
    int totalPages = jobInfoGui.calculateTotalPages(tasks, entriesPerPage);
    if (page < 1 || page > totalPages) {
      Messages.send(player, "<error>Invalid page. Valid: 1-" + totalPages);
      return 0;
    }
    displayJobInfoChat(player, job, tasks, page, entriesPerPage);
    return Command.SINGLE_SUCCESS;
  }

  private int executeGuiCommand(CommandSourceStack source, String jobName, int page) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }
    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>The job you specified does not exist.");
      return 0;
    }
    return executeGuiCommandInternal(player, job, jobService.getAllTasks(job), page);
  }

  private int executeGuiCommandInternal(
      Player player, Job job, Map<ActionType, List<JobTask>> tasks, int page) {
    int entriesPerPage = preferencesService.getEntriesPerPage(player.getUniqueId());
    int totalPages = jobInfoGui.calculateTotalPages(tasks, entriesPerPage);
    if (page < 1 || page > totalPages) {
      Messages.send(player, "<error>Invalid page. Valid: 1-" + totalPages);
      return 0;
    }
    if (!jobInfoGui.open(player, job, tasks, page)) {
      Messages.send(player, "<error>Invalid page. Valid: 1-" + totalPages);
      return 0;
    }
    return Command.SINGLE_SUCCESS;
  }

  private int setEntriesPreference(CommandSourceStack source, int count) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }
    preferencesService.setEntriesPerPage(player.getUniqueId(), count);
    Messages.send(player, "<primary>Entries per page set to <secondary>" + count + "</secondary>.");
    return Command.SINGLE_SUCCESS;
  }

  private int setGuiModePreference(CommandSourceStack source, boolean guiMode) {
    CommandSender sender = source.getSender();
    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }
    preferencesService.setGuiMode(player.getUniqueId(), guiMode);
    if (guiMode) {
      Messages.send(player, "<primary>Default view mode set to <secondary>GUI</secondary>.");
    } else {
      Messages.send(player, "<primary>Default view mode set to <secondary>Chat</secondary>.");
    }
    return Command.SINGLE_SUCCESS;
  }

  public int calculateTotalPages(Map<ActionType, List<JobTask>> tasks, int entriesPerPage) {
    return jobInfoGui.calculateTotalPages(tasks, entriesPerPage);
  }

  private void displayJobInfoChat(
      Player player, Job job, Map<ActionType, List<JobTask>> tasks, int page, int entriesPerPage) {
    final int totalPages = jobInfoGui.calculateTotalPages(tasks, entriesPerPage);
    final String jobName = job.key().value();
    String jobDisplayName = serializePlain(job.displayName());

    Messages.send(player, "");
    Messages.send(player,
        "<neutral>━━━━━━━━━ <primary>Job Info: " + jobDisplayName + " <neutral>━━━━━━━━━");
    Messages.send(player, "");
    player.sendMessage(Component.text("  ").append(job.description().color(TextColor.color(0xAEB4BF))));
    Messages.send(player, "<neutral>  Max Level: <secondary>" + job.maxLevel());
    Messages.send(player, "");

    List<Map.Entry<ActionType, List<JobTask>>> entries = new ArrayList<>(tasks.entrySet());
    int start = (page - 1) * entriesPerPage;
    int end = Math.min(start + entriesPerPage, entries.size());
    for (int i = start; i < end; i++) {
      var entry = entries.get(i);
      if (!entry.getValue().isEmpty()) {
        displayActionTypeSectionChat(player, entry.getKey(), entry.getValue());
      }
    }

    Messages.send(player, "");
    player.sendMessage(buildPaginationControls(jobName, page, totalPages));
    Messages.send(player, "");
    Messages.send(player, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Messages.send(player, "");
  }

  private void displayActionTypeSectionChat(Player player, ActionType type, List<JobTask> tasks) {
    Messages.send(player, "<neutral>  ━━ <accent>"
        + formatActionTypeName(type.name()) + "<neutral> ━━");
    for (JobTask task : tasks) {
      player.sendMessage(Component.text()
          .append(Component.text("    ● ", TextColor.color(0xAEB4BF)))
          .append(Component.text(formatContextKey(task.contextKey()), TextColor.color(0xA1E0E0)))
          .append(Component.text(" → ", TextColor.color(0xAEB4BF)))
          .append(buildPayableComponent(task.payables()))
          .build());
    }
  }

  private Component buildPaginationControls(String jobName, int currentPage, int totalPages) {
    Component controls = Component.text("  ");
    if (currentPage > 1) {
      controls = controls.append(Component.text("[◀ Previous]", TextColor.color(0xAEFFC1))
          .clickEvent(ClickEvent.runCommand("/jobs info chat " + jobName + " " + (currentPage - 1)))
          .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (currentPage - 1)))));
    } else {
      controls = controls.append(Component.text("[◀ Previous]", TextColor.color(0x555555)));
    }
    controls = controls.append(Component.text(" "));
    controls = controls.append(Component.text("Page " + currentPage + "/" + totalPages,
        TextColor.color(0xAEB4BF)));
    controls = controls.append(Component.text(" "));
    if (currentPage < totalPages) {
      controls = controls.append(Component.text("[Next ▶]", TextColor.color(0xAEFFC1))
          .clickEvent(ClickEvent.runCommand("/jobs info chat " + jobName + " " + (currentPage + 1)))
          .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (currentPage + 1)))));
    } else {
      controls = controls.append(Component.text("[Next ▶]", TextColor.color(0x555555)));
    }
    controls = controls.append(Component.text("  "));
    controls = controls.append(Component.text("[GUI]", TextColor.color(0x3FB3D5))
        .clickEvent(ClickEvent.runCommand("/jobs info gui " + jobName + " " + currentPage))
        .hoverEvent(HoverEvent.showText(Component.text("View in GUI mode"))));
    return controls;
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

  private static final PlainTextComponentSerializer PLAIN_TEXT =
      PlainTextComponentSerializer.plainText();

  private static String formatActionTypeName(String name) {
    return Arrays.stream(name.toLowerCase(java.util.Locale.ROOT).split("_"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  private static String formatContextKey(Key key) {
    String value = key.value();
    return Arrays.stream(value.split("[_/]"))
        .filter(w -> !w.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
        .collect(Collectors.joining(" "));
  }

  private static String serializePlain(Component component) {
    return PLAIN_TEXT.serialize(component);
  }
}
