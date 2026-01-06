package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.aincraft.editor.EditorService;
import net.aincraft.editor.ImportResult;
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
                Mint.sendMessage(sender, "<error>This command can only be executed by players");
                return Command.SINGLE_SUCCESS;
              }

              String code = context.getArgument("code", String.class);

              Mint.sendMessage(player, "<primary>Applying edits...");

              editorService.importTasks(code, player.getUniqueId())
                  .thenAccept(result -> {
                    // Run on main thread to safely send messages
                    org.bukkit.Bukkit.getScheduler().runTask(net.aincraft.Bridge.bridge().plugin(), () -> {
                      if (result.errors().isEmpty()) {
                        Mint.sendMessage(player, "<success>Successfully applied edits!");
                        Mint.sendMessage(player, "<accent>Tasks imported: " + result.tasksImported());
                        Mint.sendMessage(player, "<accent>Tasks deleted: " + result.tasksDeleted());
                      } else {
                        Mint.sendMessage(player, "<primary>Edits applied with errors:");
                        Mint.sendMessage(player, "<accent>Tasks imported: " + result.tasksImported());
                        Mint.sendMessage(player, "<accent>Tasks deleted: " + result.tasksDeleted());
                        Mint.sendMessage(player, "<error>Errors:");

                        for (String error : result.errors()) {
                          Mint.sendMessage(player, "<error>  - " + error);
                        }
                      }
                    });
                  })
                  .exceptionally(throwable -> {
                    // Run on main thread to safely send messages
                    org.bukkit.Bukkit.getScheduler().runTask(net.aincraft.Bridge.bridge().plugin(), () -> {
                      Mint.sendMessage(player, "<error>Failed to apply edits: " + throwable.getMessage());
                    });
                    return null;
                  });

              return Command.SINGLE_SUCCESS;
            }));
  }
}
