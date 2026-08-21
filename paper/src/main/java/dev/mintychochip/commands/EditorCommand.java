package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import dev.mintychochip.PluginProvider;
import dev.mintychochip.editor.EditorService;
import dev.mintychochip.domain.JobResolver;
import dev.mintychochip.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for the web editor.
 *
 * <p>
 * Usage:
 * <ul>
 *   <li>/jobs editor - exports all jobs to the web editor</li>
 *   <li>/jobs editor [job] - exports a specific job to the web editor</li>
 * </ul>
 */
public final class EditorCommand implements JobsCommand {

  private final EditorService editorService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  public EditorCommand(EditorService editorService, JobService jobService, JobResolver jobResolver) {
    this.editorService = editorService;
    this.jobResolver = jobResolver;
  }

  public static final String PERMISSION = AdminPermissions.ADMIN;

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("editor")
        .requires(AdminPermissions::isAdmin)
        .then(Commands.argument("job", StringArgumentType.string())
            .suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();
              if (!(sender instanceof Player player)) {
                Messages.send(sender, "<error>This command can only be used by players.");
                return Command.SINGLE_SUCCESS;
              }
              String input = context.getArgument("job", String.class);
              dev.mintychochip.Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);
              if (job == null) {
                java.util.List<String> suggestions = jobResolver.suggestSimilar(input, 3);
                Messages.send(player, "<error>Job not found: " + input);
                if (!suggestions.isEmpty()) {
                  Messages.send(player, "<neutral>Did you mean: " + String.join(", ", suggestions));
                }
                return 0;
              }
              handleExport(player, job.key().toString());
              return Command.SINGLE_SUCCESS;
            }))
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          if (!(sender instanceof Player player)) {
            Messages.send(sender, "<error>This command can only be used by players.");
            return Command.SINGLE_SUCCESS;
          }
          handleExport(player, null);
          return Command.SINGLE_SUCCESS;
        });
  }

  private void handleExport(Player player, String jobKey) {
    Messages.send(player, "<neutral>Exporting job data to web editor...");
    editorService.exportTasks(jobKey, player.getUniqueId())
        .thenAccept(result -> Bukkit.getScheduler().runTask(PluginProvider.get(), () -> {
          Component message = Component.text("Click to open editor: ")
              .append(Component.text(result.webEditorUrl())
                  .clickEvent(ClickEvent.openUrl(result.webEditorUrl())));
          player.sendMessage(message);
        }))
        .exceptionally(throwable -> {
          Bukkit.getScheduler().runTask(PluginProvider.get(), () ->
              Messages.send(player, "<error>Failed to export job data: " + throwable.getMessage()));
          return null;
        });
  }
}
