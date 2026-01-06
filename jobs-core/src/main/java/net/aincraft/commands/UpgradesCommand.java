package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.gui.UpgradeTreeGui;
import net.aincraft.service.JobResolver;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.UnlockResult;
import net.aincraft.upgrade.UpgradeTree;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for viewing and managing job upgrades.
 *
 * Usage:
 * - /jobs upgrades <job> - View upgrade tree
 * - /jobs upgrades <job> unlock <node> - Unlock a node
 * - /jobs upgrades <job> reset - Reset all upgrades
 */
public class UpgradesCommand implements JobsCommand {

  private static final String DEFAULT_NAMESPACE = "modularjobs";

  private final UpgradeService upgradeService;
  private final JobResolver jobResolver;
  private final UpgradeTreeGui upgradeTreeGui;

  @Inject
  public UpgradesCommand(
      UpgradeService upgradeService,
      JobResolver jobResolver,
      UpgradeTreeGui upgradeTreeGui) {
    this.upgradeService = upgradeService;
    this.jobResolver = jobResolver;
    this.upgradeTreeGui = upgradeTreeGui;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("upgrades")
        .then(Commands.argument("job", StringArgumentType.string())
            .suggests((context, builder) -> {
              jobResolver.getPlainNames().forEach(builder::suggest);
              return builder.buildFuture();
            })
            // View tree
            .executes(context -> executeView(
                context.getSource(),
                context.getArgument("job", String.class)))
            // Unlock subcommand
            .then(Commands.literal("unlock")
                .then(Commands.argument("node", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      String jobName = context.getArgument("job", String.class);
                      Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
                      if (job != null) {
                        upgradeService.getTree(job.key().value())
                            .ifPresent(tree -> tree.allNodes().forEach(
                                node -> builder.suggest(getShortKey(node))));
                      }
                      return builder.buildFuture();
                    })
                    .executes(context -> executeUnlock(
                        context.getSource(),
                        context.getArgument("job", String.class),
                        context.getArgument("node", String.class)))))
            // Reset subcommand
            .then(Commands.literal("reset")
                .executes(context -> executeReset(
                    context.getSource(),
                    context.getArgument("job", String.class)))));
  }

  private int executeView(CommandSourceStack source, String jobName) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Mint.sendMessage(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Mint.sendMessage(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    Optional<UpgradeTree> treeOpt = upgradeService.getTree(job.key().value());
    if (treeOpt.isEmpty()) {
      Mint.sendMessage(sender, "<neutral>This job has no upgrade tree.");
      return 0;
    }

    UpgradeTree tree = treeOpt.get();
    upgradeTreeGui.open(player, job, tree);

    return Command.SINGLE_SUCCESS;
  }

  private int executeUnlock(CommandSourceStack source, String jobName, String nodeKey) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Mint.sendMessage(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Mint.sendMessage(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);

    switch (result) {
      case UnlockResult.Success success -> {
        Mint.sendMessage(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
      }
      case UnlockResult.InsufficientPoints ip -> {
        Mint.sendMessage(player, "<error>Not enough skill points. Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
      }
      case UnlockResult.PrerequisitesNotMet pm -> {
        Mint.sendMessage(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pm.missing()));
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Mint.sendMessage(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
      }
      case UnlockResult.AlreadyUnlocked au -> {
        Mint.sendMessage(player, "<neutral>Already unlocked: " + au.nodeKey());
      }
      case UnlockResult.NodeNotFound nf -> {
        Mint.sendMessage(player, "<error>Node not found: " + nf.nodeKey());
      }
      case UnlockResult.TreeNotFound tf -> {
        Mint.sendMessage(player, "<error>No upgrade tree for job: " + tf.jobKey());
      }
    }

    return Command.SINGLE_SUCCESS;
  }

  private int executeReset(CommandSourceStack source, String jobName) {
    CommandSender sender = source.getSender();

    if (!(sender instanceof Player player)) {
      Mint.sendMessage(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Mint.sendMessage(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    boolean success = upgradeService.resetUpgrades(playerId, jobKey);
    if (success) {
      PlayerUpgradeData data = upgradeService.getPlayerData(playerId, jobKey);
      Mint.sendMessage(player, "<accent>Upgrades reset for " + job.getPlainName()
          + "<neutral>. You now have <primary>" + data.availableSkillPoints() + " SP");
    } else {
      Mint.sendMessage(player, "<error>Failed to reset upgrades.");
    }

    return Command.SINGLE_SUCCESS;
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }
}
