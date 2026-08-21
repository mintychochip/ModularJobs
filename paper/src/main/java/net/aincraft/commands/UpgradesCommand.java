package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aincraft.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.gui.UpgradeTreeGui;
import net.aincraft.domain.JobResolver;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.UnlockResult;
import net.aincraft.upgrade.UpgradeTree;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for viewing and managing job upgrades.
 *
 * <p>
 * Usage:
 * - /jobs upgrade <job> - View upgrade tree
 * - /jobs upgrade <job> unlock <node> - Unlock a node
 * - /jobs upgrade <job> reset - Reset all upgrades
 */
public class UpgradesCommand implements JobsCommand {

  private static final String DEFAULT_NAMESPACE = "modularjobs";

  private final UpgradeService upgradeService;
  private final JobResolver jobResolver;
  private final UpgradeTreeGui upgradeTreeGui;

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
    return Commands.literal("upgrade")
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
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Job job = jobResolver.resolveInNamespace(jobName, DEFAULT_NAMESPACE);
    if (job == null) {
      Messages.send(sender, "<error>Job not found: " + jobName);
      return 0;
    }

    if (!hasTreeForJob(upgradeService, job.key().value())) {
      Messages.send(sender, "<neutral>This job has no upgrade tree.");
      return 0;
    }

    Optional<UpgradeTree> treeOpt = upgradeService.getTree(job.key().value());
    UpgradeTree tree = treeOpt.orElse(null);
    upgradeTreeGui.open(player, job, tree);

    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

    return Command.SINGLE_SUCCESS;
  }

  static boolean hasTreeForJob(UpgradeService upgradeService, String jobKey) {
    return upgradeService.getTree(jobKey).isPresent()
        || upgradeService.getSkillTree(jobKey).isPresent();
  }

  private int executeUnlock(CommandSourceStack source, String jobName, String nodeKey) {
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

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    UnlockResult result = upgradeService.unlock(playerId, jobKey, nodeKey);

    switch (result) {
      case UnlockResult.Success success -> {
        Messages.send(player, "<accent>Unlocked: <primary>" + success.node().name()
            + " <neutral>(<secondary>" + success.remainingPoints() + " SP remaining<neutral>)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
      }
      case UnlockResult.InsufficientPoints ip -> {
        Messages.send(player, "<error>Not enough skill points. Need <secondary>" + ip.required()
            + "<error>, have <secondary>" + ip.available());
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.PrerequisitesNotMet pm -> {
        Messages.send(player, "<error>Missing prerequisites: <secondary>" + String.join(", ", pm.missing()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.ExcludedByChoice ec -> {
        Messages.send(player, "<error>Blocked by: <secondary>" + String.join(", ", ec.conflicting()));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
      case UnlockResult.AlreadyUnlocked au -> {
        Messages.send(player, "<neutral>Already unlocked: " + au.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case UnlockResult.NodeNotFound nf -> {
        Messages.send(player, "<error>Node not found: " + nf.nodeKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
      case UnlockResult.TreeNotFound tf -> {
        Messages.send(player, "<error>No upgrade tree for job: " + tf.jobKey());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
      }
    }

    return Command.SINGLE_SUCCESS;
  }

  private int executeReset(CommandSourceStack source, String jobName) {
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

    String playerId = player.getUniqueId().toString();
    String jobKey = job.key().value();

    boolean success = upgradeService.resetUpgrades(playerId, jobKey);
    if (success) {
      PlayerUpgradeData data = upgradeService.getPlayerData(playerId, jobKey);
      Messages.send(player, "<accent>Upgrades reset for " + job.getPlainName()
          + "<neutral>. You now have <primary>" + data.availableSkillPoints() + " SP");
      player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
    } else {
      Messages.send(player, "<error>Failed to reset upgrades.");
      player.playSound(player, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
    }

    return Command.SINGLE_SUCCESS;
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }
}
