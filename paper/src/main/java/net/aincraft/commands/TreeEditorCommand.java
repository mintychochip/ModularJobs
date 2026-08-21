package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aincraft.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.domain.JobResolver;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.config.UpgradeTreeLoader;
import net.aincraft.upgrade.editor.TreeEditorGui;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for editing upgrade trees.
 *
 * <p>
 * Usage:
 * - /jobs treeeditor <job> - Edit existing tree
 * - /jobs treeeditor new <job> - Create new tree
 */
public class TreeEditorCommand implements JobsCommand {

  private static final String DEFAULT_NAMESPACE = "modularjobs";

  private final UpgradeService upgradeService;
  private final JobResolver jobResolver;
  private final TreeEditorGui treeEditorGui;
  private final UpgradeTreeLoader treeLoader;

  public TreeEditorCommand(
      UpgradeService upgradeService,
      JobResolver jobResolver,
      TreeEditorGui treeEditorGui,
      UpgradeTreeLoader treeLoader) {
    this.upgradeService = upgradeService;
    this.jobResolver = jobResolver;
    this.treeEditorGui = treeEditorGui;
    this.treeLoader = treeLoader;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("treeeditor")
        .requires(source -> source.getSender().hasPermission("jobs.command.admin.treeeditor"))
        // Edit existing tree: /jobs treeeditor <job>
        .then(Commands.argument("job", StringArgumentType.string())
            .suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
            .executes(context -> executeEdit(
                context.getSource(),
                context.getArgument("job", String.class))))
        // Create new tree: /jobs treeeditor new <job>
        .then(Commands.literal("new")
            .then(Commands.argument("job", StringArgumentType.string())
                .suggests((context, builder) -> {
                  jobResolver.getPlainNames().forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .executes(context -> executeNew(
                    context.getSource(),
                    context.getArgument("job", String.class)))));
  }

  private int executeEdit(CommandSourceStack source, String jobName) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    // Try to reload from file first to get latest changes
    String treeId = job.key().value(); // e.g., "modularjobs:alchemist" -> "alchemist"
    String simpleTreeId = treeId.substring(treeId.lastIndexOf(':') + 1); // Extract "alchemist"

    Optional<UpgradeTree> treeOpt = treeLoader.loadSingleTree(simpleTreeId);
    if (treeOpt.isEmpty()) {
      // Fall back to in-memory tree if file doesn't exist
      treeOpt = upgradeService.getTree(treeId);
      if (treeOpt.isEmpty()) {
        Messages.send(sender, "<neutral>This job has no upgrade tree. Use <secondary>/jobs treeeditor new " + jobName + "<neutral> to create one.");
        return 0;
      }
    }

    UpgradeTree tree = treeOpt.get();
    treeEditorGui.open(player, tree);
    Messages.send(player, "<accent>Opening tree editor for <primary>" + job.getPlainName() + " <neutral>(loaded from file)");
    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

    return Command.SINGLE_SUCCESS;
  }

  private int executeNew(CommandSourceStack source, String jobName) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    // Check if tree already exists
    Optional<UpgradeTree> existingTree = upgradeService.getTree(job.key().value());
    if (existingTree.isPresent()) {
      Messages.send(sender, "<warning>A tree already exists for this job. Use <secondary>/jobs treeeditor " + jobName + "<warning> to edit it.");
      return 0;
    }

    treeEditorGui.openNew(player, job.key().value());
    Messages.send(player, "<accent>Creating new tree for <primary>" + job.getPlainName());
    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);

    return Command.SINGLE_SUCCESS;
  }
}
