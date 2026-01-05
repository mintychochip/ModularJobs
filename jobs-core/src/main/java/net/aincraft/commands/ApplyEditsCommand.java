package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.editor.EditorService;
import net.aincraft.editor.ImportResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class ApplyEditsCommand implements JobsCommand {

  private final EditorService editorService;

  @Inject
  ApplyEditsCommand(EditorService editorService) {
    this.editorService = editorService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("applyedits")
        .then(Commands.argument("code", StringArgumentType.string())
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be executed by players")
                    .color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
              }

              String code = context.getArgument("code", String.class);

              player.sendMessage(Component.text("Applying edits...")
                  .color(NamedTextColor.YELLOW));

              editorService.importTasks(code, player.getUniqueId())
                  .thenAccept(result -> {
                    // Run on main thread to safely send messages
                    org.bukkit.Bukkit.getScheduler().runTask(net.aincraft.Bridge.bridge().plugin(), () -> {
                      if (result.errors().isEmpty()) {
                        player.sendMessage(Component.text("Successfully applied edits!")
                            .color(NamedTextColor.GREEN)
                            .append(Component.newline())
                            .append(Component.text("Tasks imported: " + result.tasksImported())
                                .color(NamedTextColor.GREEN))
                            .append(Component.newline())
                            .append(Component.text("Tasks deleted: " + result.tasksDeleted())
                                .color(NamedTextColor.GREEN)));
                      } else {
                        Component errorMessage = Component.text("Edits applied with errors:")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.newline())
                            .append(Component.text("Tasks imported: " + result.tasksImported())
                                .color(NamedTextColor.GREEN))
                            .append(Component.newline())
                            .append(Component.text("Tasks deleted: " + result.tasksDeleted())
                                .color(NamedTextColor.GREEN))
                            .append(Component.newline())
                            .append(Component.text("Errors:").color(NamedTextColor.RED));

                        for (String error : result.errors()) {
                          errorMessage = errorMessage.append(Component.newline())
                              .append(Component.text("  - " + error).color(NamedTextColor.RED));
                        }

                        player.sendMessage(errorMessage);
                      }
                    });
                  })
                  .exceptionally(throwable -> {
                    // Run on main thread to safely send messages
                    org.bukkit.Bukkit.getScheduler().runTask(net.aincraft.Bridge.bridge().plugin(), () -> {
                      player.sendMessage(Component.text("Failed to apply edits: " + throwable.getMessage())
                          .color(NamedTextColor.RED));
                    });
                    return null;
                  });

              return Command.SINGLE_SUCCESS;
            }));
  }
}
