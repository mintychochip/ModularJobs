package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import dev.mintychochip.PluginProvider;
import dev.mintychochip.editor.EditorService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Imports web-editor payloads (task edits) into live job definitions.
 * Admin-only; requires a player sender and applies the imported tasks
 * asynchronously, reporting results back on the main thread.
 */
public final class ApplyEditsCommand implements JobsCommand {

  private final EditorService editorService;

  /**
   * Creates the apply-edits command backed by the editor import service.
   *
   * @param editorService service used to import web-editor task payloads
   */
  public ApplyEditsCommand(EditorService editorService) {
    this.editorService = editorService;
  }

  /** Admin permission required to import web-editor payloads into live job tasks. */
  public static final String PERMISSION = AdminPermissions.ADMIN;

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("applyedits")
        .requires(AdminPermissions::isAdmin)
        .then(Commands.argument("code", StringArgumentType.string())
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                Messages.send(sender, "<error>This command can only be executed by players");
                return Command.SINGLE_SUCCESS;
              }

              String code = context.getArgument("code", String.class);

              Messages.send(player, "<primary>Applying edits...");

              editorService.importTasks(code, player.getUniqueId())
                  .thenAccept(result -> {
                    // Run on main thread to safely send messages
                    Bukkit.getScheduler().runTask(PluginProvider.get(), () -> {
                      if (result.errors().isEmpty()) {
                        Messages.send(player, "<success>Successfully applied edits!");
                        Messages.send(player, "<accent>Tasks imported: " + result.tasksImported());
                        Messages.send(player, "<accent>Tasks deleted: " + result.tasksDeleted());
                      } else {
                        Messages.send(player, "<primary>Edits applied with errors:");
                        Messages.send(player, "<accent>Tasks imported: " + result.tasksImported());
                        Messages.send(player, "<accent>Tasks deleted: " + result.tasksDeleted());
                        Messages.send(player, "<error>Errors:");

                        for (String error : result.errors()) {
                          Messages.send(player, "<error>  - " + error);
                        }
                      }
                    });
                  })
                  .exceptionally(throwable -> {
                    // Run on main thread to safely send messages
                    Bukkit.getScheduler().runTask(PluginProvider.get(), () -> {
                      Messages.send(player, "<error>Failed to apply edits: " + throwable.getMessage());
                    });
                    return null;
                  });

              return Command.SINGLE_SUCCESS;
            }));
  }
}
