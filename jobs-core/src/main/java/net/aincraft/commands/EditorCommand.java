package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.Bridge;
import net.aincraft.editor.EditorService;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for the web editor.
 * <p>
 * Usage:
 * <ul>
 *   <li>/jobs editor - exports all jobs to the web editor</li>
 *   <li>/jobs editor [job] - exports a specific job to the web editor</li>
 * </ul>
 */
final class EditorCommand implements JobsCommand {

  private final EditorService editorService;
  private final JobService jobService;
  private final JobResolver jobResolver;
  private static final String DEFAULT_NAMESPACE = "modularjobs";

  @Inject
  public EditorCommand(EditorService editorService, JobService jobService, JobResolver jobResolver) {
    this.editorService = editorService;
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("editor")
        // /jobs editor [job] - with optional job argument
        .then(Commands.argument("job", StringArgumentType.string())
            .suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.")
                    .color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
              }

              String input = context.getArgument("job", String.class);

              // Resolve job (supports both plain name and full key)
              net.aincraft.Job job = jobResolver.resolveInNamespace(input, DEFAULT_NAMESPACE);

              if (job == null) {
                // Try fuzzy matching for suggestions
                java.util.List<String> suggestions = jobResolver.suggestSimilar(input, 3);

                if (suggestions.isEmpty()) {
                  player.sendMessage(Component.text("Job not found: " + input)
                      .color(NamedTextColor.RED));
                } else {
                  player.sendMessage(Component.text("Job not found: " + input)
                      .color(NamedTextColor.RED));
                  player.sendMessage(Component.text("Did you mean: " + String.join(", ", suggestions))
                      .color(NamedTextColor.GRAY));
                }
                return 0;
              }

              handleExport(player, job.key().toString());
              return Command.SINGLE_SUCCESS;
            }))
        // /jobs editor - without arguments (export all jobs)
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();

          if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
          }

          handleExport(player, null);
          return Command.SINGLE_SUCCESS;
        });
  }

  /**
   * Handles the export operation for a player.
   *
   * @param player the player performing the export
   * @param jobKey the job key to export, or null to export all jobs
   */
  private void handleExport(Player player, String jobKey) {
    player.sendMessage(Component.text("Exporting job data to web editor...")
        .color(NamedTextColor.GRAY));

    editorService.exportTasks(jobKey, player.getUniqueId())
        .thenAccept(result -> {
          // Run on main thread to safely send messages
          Bukkit.getScheduler().runTask(Bridge.bridge().plugin(), () -> {
            Component message = Component.text("Click to open editor: ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(result.webEditorUrl())
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(result.webEditorUrl())));

            player.sendMessage(message);
          });
        })
        .exceptionally(throwable -> {
          // Run on main thread to safely send messages
          Bukkit.getScheduler().runTask(Bridge.bridge().plugin(), () -> {
            player.sendMessage(Component.text("Failed to export job data: " + throwable.getMessage())
                .color(NamedTextColor.RED));
          });
          return null;
        });
  }
}
