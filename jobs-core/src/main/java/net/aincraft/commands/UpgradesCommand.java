package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.config.ColorScheme;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeService.UnlockResult;
import net.aincraft.upgrade.UpgradeTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
  private static final int DIALOG_WIDTH = 1000;

  private final UpgradeService upgradeService;
  private final JobService jobService;
  private final JobResolver jobResolver;
  private final ColorScheme colors;

  @Inject
  public UpgradesCommand(
      UpgradeService upgradeService,
      JobService jobService,
      JobResolver jobResolver,
      ColorScheme colors) {
    this.upgradeService = upgradeService;
    this.jobService = jobService;
    this.jobResolver = jobResolver;
    this.colors = colors;
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
    PlayerUpgradeData data = upgradeService.getPlayerData(
        player.getUniqueId().toString(), job.key().value());

    Dialog dialog = buildTreeDialog(job, tree, data);
    player.showDialog(dialog);

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

  private Dialog buildTreeDialog(Job job, UpgradeTree tree, PlayerUpgradeData data) {
    List<DialogBody> bodies = new ArrayList<>();

    // Header
    bodies.add(buildHeader(job, tree, data));
    bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));

    // Group nodes by unlock status
    Set<String> unlocked = data.unlockedNodes();
    Set<UpgradeNode> available = tree.getAvailableNodes(unlocked);
    Collection<UpgradeNode> allNodes = tree.allNodes();

    // Show unlocked nodes
    List<UpgradeNode> unlockedNodes = allNodes.stream()
        .filter(n -> unlocked.contains(getShortKey(n)))
        .toList();
    if (!unlockedNodes.isEmpty()) {
      bodies.add(buildSectionHeader("Unlocked", colors.accent()));
      for (UpgradeNode node : unlockedNodes) {
        bodies.add(buildNodeEntry(node, NodeStatus.UNLOCKED));
      }
      bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));
    }

    // Show available nodes
    if (!available.isEmpty()) {
      bodies.add(buildSectionHeader("Available", colors.secondary()));
      for (UpgradeNode node : available) {
        bodies.add(buildNodeEntry(node, NodeStatus.AVAILABLE));
      }
      bodies.add(DialogBody.plainMessage(Component.empty(), DIALOG_WIDTH));
    }

    // Show locked nodes
    List<UpgradeNode> lockedNodes = allNodes.stream()
        .filter(n -> !unlocked.contains(getShortKey(n)) && !available.contains(n))
        .toList();
    if (!lockedNodes.isEmpty()) {
      bodies.add(buildSectionHeader("Locked", colors.neutral()));
      for (UpgradeNode node : lockedNodes) {
        bodies.add(buildNodeEntry(node, NodeStatus.LOCKED));
      }
    }

    DialogBase base = DialogBase.create(
        Component.text()
            .append(Component.text("Upgrades: ", colors.neutral()))
            .append(job.displayName())
            .build(),
        null,
        true,
        false,
        DialogBase.DialogAfterAction.CLOSE,
        bodies,
        List.of()
    );

    // Close button
    ActionButton closeButton = ActionButton.builder(Component.text("Close", colors.neutral()))
        .width(300)
        .action(null)
        .build();

    return Dialog.create(builder -> builder.empty()
        .base(base)
        .type(DialogType.multiAction(List.of(closeButton)).build())
    );
  }

  private DialogBody buildHeader(Job job, UpgradeTree tree, PlayerUpgradeData data) {
    Component header = Component.text()
        .append(job.displayName())
        .append(Component.newline())
        .append(Component.text("Skill Points: ", colors.neutral()))
        .append(Component.text(data.availableSkillPoints(), colors.primary()))
        .append(Component.text(" / ", colors.neutral()))
        .append(Component.text(data.totalSkillPoints(), colors.accent()))
        .append(Component.newline())
        .append(Component.text("Unlocked: ", colors.neutral()))
        .append(Component.text(data.unlockedNodes().size(), colors.secondary()))
        .append(Component.text(" / ", colors.neutral()))
        .append(Component.text(tree.allNodes().size(), colors.accent()))
        .append(Component.newline())
        .append(Component.text("Points per level: ", colors.neutral()))
        .append(Component.text(tree.skillPointsPerLevel(), colors.accent()))
        .build();

    return DialogBody.plainMessage(header, DIALOG_WIDTH);
  }

  private DialogBody buildSectionHeader(String title, TextColor color) {
    return DialogBody.plainMessage(
        Component.text()
            .append(Component.text("━━ ", colors.neutral()))
            .append(Component.text(title, color).decorate(TextDecoration.BOLD))
            .append(Component.text(" ━━", colors.neutral()))
            .build(),
        DIALOG_WIDTH
    );
  }

  private DialogBody buildNodeEntry(UpgradeNode node, NodeStatus status) {
    TextColor statusColor = switch (status) {
      case UNLOCKED -> colors.accent();
      case AVAILABLE -> colors.secondary();
      case LOCKED -> colors.neutral();
    };

    String statusIcon = switch (status) {
      case UNLOCKED -> "[x]";
      case AVAILABLE -> "[ ]";
      case LOCKED -> "[-]";
    };

    TextComponent.Builder builder = Component.text()
        .append(Component.text("  " + statusIcon + " ", statusColor))
        .append(Component.text(node.name(), colors.primary()))
        .append(Component.text(" (", colors.neutral()))
        .append(Component.text(node.cost() + " SP", colors.secondary()))
        .append(Component.text(")", colors.neutral()));

    // Add description if present
    if (node.description() != null && !node.description().isEmpty()) {
      builder.append(Component.newline())
          .append(Component.text("      ", colors.neutral()))
          .append(Component.text(node.description(), colors.neutral()));
    }

    // Add effects summary
    if (!node.effects().isEmpty()) {
      builder.append(Component.newline())
          .append(Component.text("      Effects: ", colors.neutral()))
          .append(formatEffects(node.effects()));
    }

    // Show prerequisites for locked nodes
    if (status == NodeStatus.LOCKED && !node.prerequisites().isEmpty()) {
      builder.append(Component.newline())
          .append(Component.text("      Requires: ", colors.neutral()))
          .append(Component.text(String.join(", ", node.prerequisites()), colors.secondary()));
    }

    return DialogBody.plainMessage(builder.build(), DIALOG_WIDTH);
  }

  private Component formatEffects(List<UpgradeEffect> effects) {
    return Component.text(effects.stream()
        .map(this::formatEffect)
        .collect(Collectors.joining(", ")), colors.accent());
  }

  private String formatEffect(UpgradeEffect effect) {
    return switch (effect) {
      case UpgradeEffect.BoostEffect boost ->
          String.format("+%.0f%% %s", (boost.multiplier().doubleValue() - 1) * 100, boost.target());
      case UpgradeEffect.PassiveEffect passive ->
          passive.ability();
      case UpgradeEffect.StatEffect stat ->
          String.format("%s +%d", stat.stat(), stat.value());
      case UpgradeEffect.UnlockEffect unlock ->
          String.format("Unlock: %s", unlock.unlockKey());
    };
  }

  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }

  private enum NodeStatus {
    UNLOCKED, AVAILABLE, LOCKED
  }
}
