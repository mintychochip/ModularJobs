package net.aincraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.aincraft.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.ConditionTreeFormatter;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.SlotSetParser;
import net.aincraft.boost.config.BoostSourceLoader;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.service.ItemBoostDataService;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.JobProgression;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.aincraft.util.DurationParser;
import java.util.Map;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Unified boost management command.
 * <p>
 * Subcommands:
 * <ul>
 *   <li>/jobs boost apply &lt;target&gt; &lt;source&gt; &lt;duration&gt; - Apply timed boost</li>
 *   <li>/jobs boost remove &lt;target&gt; &lt;source&gt; - Remove active boost</li>
 *   <li>/jobs boost list [target] - List active boosts</li>
 *   <li>/jobs boost item &lt;source&gt; &lt;duration&gt; [amount] - Create consumable item</li>
 *   <li>/jobs boost sources - List/reload boost sources</li>
 * </ul>
 */
public final class BoostCommand implements JobsCommand {

  private static final String GLOBAL_TARGET = "@global";
  private static final String ALL_TARGET = "@all";

  private final Registry<BoostSource> boostSourceRegistry;
  private final TimedBoostDataService timedBoostDataService;
  private final ItemBoostDataService itemBoostDataService;
  private final BoostSourceLoader boostSourceLoader;
  private final UpgradeBoostDataService upgradeBoostDataService;
  private final JobService jobService;

  public BoostCommand(
      Registry<BoostSource> boostSourceRegistry,
      TimedBoostDataService timedBoostDataService,
      ItemBoostDataService itemBoostDataService,
      BoostSourceLoader boostSourceLoader,
      UpgradeBoostDataService upgradeBoostDataService,
      JobService jobService
  ) {
    this.boostSourceRegistry = boostSourceRegistry;
    this.timedBoostDataService = timedBoostDataService;
    this.itemBoostDataService = itemBoostDataService;
    this.boostSourceLoader = boostSourceLoader;
    this.upgradeBoostDataService = upgradeBoostDataService;
    this.jobService = jobService;
  }

  /** Admin permission required for all boost management subcommands. */
  public static final String PERMISSION = AdminPermissions.ADMIN;

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("boost")
        .requires(AdminPermissions::isAdmin)
        .then(buildApplyCommand())
        .then(buildRemoveCommand())
        .then(buildListCommand())
        .then(buildItemCommand())
        .then(buildSourcesCommand());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // APPLY COMMAND
  // ─────────────────────────────────────────────────────────────────────────────

  private LiteralArgumentBuilder<CommandSourceStack> buildApplyCommand() {
    return Commands.literal("apply")
        .then(Commands.argument("target", StringArgumentType.word())
            .suggests((context, builder) -> {
              builder.suggest(GLOBAL_TARGET);
              builder.suggest(ALL_TARGET);
              Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
              return builder.buildFuture();
            })
            .then(Commands.argument("source", ArgumentTypes.key())
                .suggests((context, builder) -> {
                  boostSourceRegistry.stream()
                      .map(s -> s.key().asString())
                      .forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .then(Commands.argument("duration", StringArgumentType.greedyString())
                    .executes(this::executeApply)
                )
            )
        );
  }

  private int executeApply(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    String targetStr = context.getArgument("target", String.class);
    Key sourceKey = context.getArgument("source", Key.class);
    String durationStr = context.getArgument("duration", String.class);

    // Validate boost source
    BoostSource boostSource = boostSourceRegistry.get(sourceKey).orElse(null);
    if (boostSource == null) {
      Messages.send(sender,
          "<error>Unknown boost source: <secondary>" + sourceKey.asString());
      return 0;
    }

    // Parse duration
    Duration duration;
    try {
      duration = DurationParser.parse(durationStr);
    } catch (IllegalArgumentException e) {
      Messages.send(sender, "<error>Invalid duration: <secondary>" + e.getMessage());
      return 0;
    }

    ConsumableBoostData boostData = new ConsumableBoostData(boostSource, duration);

    // Apply based on target type
    if (GLOBAL_TARGET.equalsIgnoreCase(targetStr)) {
      timedBoostDataService.addData(boostData, new GlobalTarget());
      Messages.send(sender,
          "<accent>Applied <primary>" + sourceKey.asString() + "<accent> globally for <secondary>"
              + DurationParser.format(duration));

    } else if (ALL_TARGET.equalsIgnoreCase(targetStr)) {
      int count = 0;
      for (Player player : Bukkit.getOnlinePlayers()) {
        timedBoostDataService.addData(boostData, new PlayerTarget(player.getUniqueId()));
        count++;
      }
      Messages.send(sender,
          "<accent>Applied <primary>" + sourceKey.asString() + "<accent> to <secondary>" + count
              + " player(s)<accent> for <secondary>" + DurationParser.format(duration));

    } else {
      Player target = Bukkit.getPlayer(targetStr);
      if (target == null) {
        Messages.send(sender, "<error>Player not found: <secondary>" + targetStr);
        return 0;
      }
      timedBoostDataService.addData(boostData, new PlayerTarget(target.getUniqueId()));
      Messages.send(sender,
          "<accent>Applied <primary>" + sourceKey.asString() + "<accent> to <secondary>"
              + target.getName() + "<accent> for <secondary>" + DurationParser.format(duration));
    }

    return Command.SINGLE_SUCCESS;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // REMOVE COMMAND
  // ─────────────────────────────────────────────────────────────────────────────

  private LiteralArgumentBuilder<CommandSourceStack> buildRemoveCommand() {
    return Commands.literal("remove")
        .then(Commands.argument("target", StringArgumentType.word())
            .suggests((context, builder) -> {
              builder.suggest(GLOBAL_TARGET);
              Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
              return builder.buildFuture();
            })
            .then(Commands.argument("source", ArgumentTypes.key())
                .suggests((context, builder) -> {
                  boostSourceRegistry.stream()
                      .map(s -> s.key().asString())
                      .forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .executes(this::executeRemove)
            )
        );
  }

  private int executeRemove(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    String targetStr = context.getArgument("target", String.class);
    Key sourceKey = context.getArgument("source", Key.class);

    String targetDisplay;
    TimedBoostDataService.Target target;

    if (GLOBAL_TARGET.equalsIgnoreCase(targetStr)) {
      target = new GlobalTarget();
      targetDisplay = "global";
    } else {
      Player player = Bukkit.getPlayer(targetStr);
      if (player == null) {
        Messages.send(sender, "<error>Player not found: <secondary>" + targetStr);
        return 0;
      }
      target = new PlayerTarget(player.getUniqueId());
      targetDisplay = player.getName();
    }

    boolean removed = timedBoostDataService.removeBoost(target, sourceKey.asString());

    if (removed) {
      Messages.send(sender,
          "<accent>Removed <primary>" + sourceKey.asString() + "<accent> from <secondary>"
              + targetDisplay);
    } else {
      Messages.send(sender, "<error>No active boost <secondary>" + sourceKey.asString()
          + "<error> found for <secondary>" + targetDisplay);
    }

    return Command.SINGLE_SUCCESS;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // LIST COMMAND
  // ─────────────────────────────────────────────────────────────────────────────

  private LiteralArgumentBuilder<CommandSourceStack> buildListCommand() {
    return Commands.literal("list")
        .executes(context -> executeList(context, null))
        .then(Commands.argument("target", StringArgumentType.word())
            .suggests((context, builder) -> {
              builder.suggest(GLOBAL_TARGET);
              Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
              return builder.buildFuture();
            })
            .executes(context -> executeList(context, context.getArgument("target", String.class)))
        );
  }

  private int executeList(CommandContext<CommandSourceStack> context, String targetStr) {
    CommandSender sender = context.getSource().getSender();

    // Default to self if player, otherwise require target
    if (targetStr == null) {
      if (!(sender instanceof Player player)) {
        Messages.send(sender,
            "<error>Specify a target: <secondary>/jobs boost list <player|@global>");
        return 0;
      }
      return listPlayerBoosts(sender, player);
    }

    if (GLOBAL_TARGET.equalsIgnoreCase(targetStr)) {
      return listGlobalBoosts(sender);
    }

    Player target = Bukkit.getPlayer(targetStr);
    if (target == null) {
      Messages.send(sender, "<error>Player not found: <secondary>" + targetStr);
      return 0;
    }

    return listPlayerBoosts(sender, target);
  }

  private int listPlayerBoosts(CommandSender sender, Player player) {
    Messages.send(sender,
        "<neutral>━━━━━━━━━ <primary>Boosts: " + player.getName() + "<neutral> ━━━━━━━━━");
    Messages.send(sender, "");

    // Timed boosts
    List<ActiveBoostData> timedBoosts = timedBoostDataService.findApplicableBoosts(
        new PlayerTarget(player.getUniqueId()));

    if (!timedBoosts.isEmpty()) {
      Messages.send(sender, "<secondary>Timed Boosts:");
      for (ActiveBoostData boost : timedBoosts) {
        String remaining = DurationParser.formatRemaining(boost.started().getTime(),
            boost.duration());
        String boostEffects = formatBoostEffects(boost.boostSource());
        Messages.send(sender,
            "<neutral>  - <accent>" + boost.boostSource().key().asString());
        Messages.send(sender,
            "<neutral>      <secondary>" + boostEffects + " <neutral>[" + remaining + "]");
      }
      Messages.send(sender, "");
    }

    // Passive item boosts
    List<PassiveBoostInfo> passiveBoosts = getPassiveBoosts(player);
    if (!passiveBoosts.isEmpty()) {
      Messages.send(sender, "<secondary>Passive Boosts:");
      for (PassiveBoostInfo info : passiveBoosts) {
        String boostEffects = formatBoostEffects(info.boostSource);
        Messages.send(sender,
            "<neutral>  - <accent>" + info.boostSource.key().asString() + " <neutral>(slot "
                + info.slot + ")");
        Messages.send(sender, "<neutral>      <secondary>" + boostEffects);
      }
      Messages.send(sender, "");
    }

    // Upgrade tree boosts (now uses the same BoostSource API)
    List<JobProgression> progressions = jobService.getProgressions(player.getUniqueId());
    boolean hasUpgradeBoosts = false;

    for (JobProgression progression : progressions) {
      List<BoostSource> upgradeBoosts = upgradeBoostDataService.getBoostSources(
          player.getUniqueId(), progression.job().key());

      if (!upgradeBoosts.isEmpty()) {
        if (!hasUpgradeBoosts) {
          Messages.send(sender, "<secondary>Upgrade Boosts:");
          hasUpgradeBoosts = true;
        }

        String jobName = progression.job().key().value();
        for (BoostSource source : upgradeBoosts) {
          String boostEffects = formatBoostEffects(source);
          String desc = source.description() != null ? source.description() : source.key().value();
          Messages.send(sender,
              "<neutral>  - <accent>" + jobName + " <neutral>(" + desc + "): <secondary>" + boostEffects);
        }
      }
    }

    if (hasUpgradeBoosts) {
      Messages.send(sender, "");
    }

    if (timedBoosts.isEmpty() && passiveBoosts.isEmpty() && !hasUpgradeBoosts) {
      Messages.send(sender, "<neutral>  No active boosts.");
      Messages.send(sender, "");
    }

    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    return Command.SINGLE_SUCCESS;
  }

  private int listGlobalBoosts(CommandSender sender) {
    Messages.send(sender, "<neutral>━━━━━━━━━ <primary>Global Boosts<neutral> ━━━━━━━━━");
    Messages.send(sender, "");

    List<ActiveBoostData> globalBoosts = timedBoostDataService.findBoosts(new GlobalTarget());

    if (globalBoosts.isEmpty()) {
      Messages.send(sender, "<neutral>  No global boosts active.");
    } else {
      for (ActiveBoostData boost : globalBoosts) {
        if (boost.isExpired()) {
          continue;
        }
        String remaining = DurationParser.formatRemaining(boost.started().getTime(),
            boost.duration());
        String boostEffects = formatBoostEffects(boost.boostSource());
        Messages.send(sender,
            "<neutral>  - <accent>" + boost.boostSource().key().asString());
        Messages.send(sender,
            "<neutral>      <secondary>" + boostEffects + " <neutral>[" + remaining + "]");
      }
    }

    Messages.send(sender, "");
    Messages.send(sender, "<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    return Command.SINGLE_SUCCESS;
  }

  private List<PassiveBoostInfo> getPassiveBoosts(Player player) {
    List<PassiveBoostInfo> passiveBoosts = new ArrayList<>();
    Set<String> seenBoostKeys = new HashSet<>();
    PlayerInventory inventory = player.getInventory();

    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      if (item == null) {
        continue;
      }

      Optional<SerializableBoostData> dataOpt = itemBoostDataService.getData(item);
      if (dataOpt.isEmpty()) {
        continue;
      }

      if (dataOpt.get() instanceof PassiveBoostData passiveData) {
        BitSet slotSet = passiveData.slotSet();
        if (slotSet.get(slot)) {
          BoostSource source = passiveData.boostSource();
          String key = source.key().asString();
          if (!seenBoostKeys.contains(key)) {
            passiveBoosts.add(new PassiveBoostInfo(source, slot));
            seenBoostKeys.add(key);
          }
        }
      }
    }
    return passiveBoosts;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // ITEM COMMAND
  // ─────────────────────────────────────────────────────────────────────────────

  private LiteralArgumentBuilder<CommandSourceStack> buildItemCommand() {
    return Commands.literal("item")
        .then(Commands.argument("source", ArgumentTypes.key())
            .suggests((context, builder) -> {
              boostSourceRegistry.stream()
                  .map(s -> s.key().asString())
                  .forEach(builder::suggest);
              return builder.buildFuture();
            })
            .then(Commands.argument("duration", StringArgumentType.word())
                .executes(context -> executeItem(context, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(context -> executeItem(context,
                        context.getArgument("amount", Integer.class)))
                )
            )
        );
  }

  private int executeItem(CommandContext<CommandSourceStack> context, int amount) {
    CommandSender sender = context.getSource().getSender();

    if (!(sender instanceof Player player)) {
      Messages.send(sender, "<error>This command can only be used by players.");
      return 0;
    }

    Key sourceKey = context.getArgument("source", Key.class);
    String durationStr = context.getArgument("duration", String.class);

    // Validate boost source
    BoostSource boostSource = boostSourceRegistry.get(sourceKey).orElse(null);
    if (boostSource == null) {
      Messages.send(sender,
          "<error>Unknown boost source: <secondary>" + sourceKey.asString());
      return 0;
    }

    // Parse duration
    Duration duration;
    try {
      duration = DurationParser.parse(durationStr);
    } catch (IllegalArgumentException e) {
      Messages.send(sender, "<error>Invalid duration: <secondary>" + e.getMessage());
      return 0;
    }

    // Create consumable item (using golden apple as default consumable)
    ItemStack item = new ItemStack(Material.GOLDEN_APPLE, amount);
    ConsumableBoostData boostData = new ConsumableBoostData(boostSource, duration);
    itemBoostDataService.addData(boostData, item);

    // Add lore using Mint tags internally (will be parsed by Adventure)
    String desc = boostSource.description();
    List<Component> lore = new ArrayList<>();
    lore.add(Component.text("Boost: " + sourceKey.asString()));
    lore.add(Component.text("Duration: " + DurationParser.format(duration)));
    if (desc != null && !desc.isEmpty()) {
      lore.add(Component.empty());
      lore.add(Component.text(desc));
    }
    item.lore(lore);

    // Set display name
    item.editMeta(meta -> {
      meta.displayName(Component.text("Boost: " + sourceKey.value()));
    });

    player.getInventory().addItem(item);

    Messages.send(sender,
        "<accent>Created <secondary>" + amount + "x<primary> " + sourceKey.asString()
            + "<accent> consumable(s) [<secondary>" + DurationParser.format(duration)
            + "<accent>]");

    return Command.SINGLE_SUCCESS;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // SOURCES COMMAND
  // ─────────────────────────────────────────────────────────────────────────────

  private LiteralArgumentBuilder<CommandSourceStack> buildSourcesCommand() {
    return Commands.literal("sources")
        .executes(context -> listSources(context.getSource()))
        .then(Commands.literal("reload")
            .executes(context -> reloadSources(context.getSource()))
        )
        .then(Commands.literal("info")
            .then(Commands.argument("source", ArgumentTypes.key())
                .suggests((context, builder) -> {
                  boostSourceRegistry.stream()
                      .map(s -> s.key().asString())
                      .forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .executes(context -> showSourceInfo(context.getSource(),
                    context.getArgument("source", Key.class)))
            )
        );
  }

  private int listSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();
    var sources = boostSourceRegistry.stream().toList();

    if (sources.isEmpty()) {
      Messages.send(sender, "<secondary>No boost sources registered.");
      return 0;
    }

    Messages.send(sender,
        "<neutral>━━━━ <primary>Boost Sources (" + sources.size() + ")<neutral> ━━━━");

    for (BoostSource bs : sources) {
      String message = "<neutral>  - <accent>" + bs.key().asString();

      String desc = bs.description();
      if (desc != null && !desc.isEmpty()) {
        message += "\n<neutral>      " + desc;
      }
      Messages.send(sender, message);
    }

    return Command.SINGLE_SUCCESS;
  }

  private int reloadSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();
    Messages.send(sender, "<secondary>Reloading boost sources...");

    int count = boostSourceLoader.reload();

    Messages.send(sender,
        "<accent>Reloaded <primary>" + count + "<accent> boost source(s).");

    return Command.SINGLE_SUCCESS;
  }

  private int showSourceInfo(CommandSourceStack source, Key sourceKey) {
    CommandSender sender = source.getSender();

    BoostSource boostSource = boostSourceRegistry.get(sourceKey).orElse(null);
    if (boostSource == null) {
      Messages.send(sender,
          "<error>Boost source not found: <secondary>" + sourceKey.asString());
      return 0;
    }

    Messages.send(sender, "<neutral>━━━━ <primary>Boost Source Info<neutral> ━━━━");

    Messages.send(sender, "<neutral>Key: <accent>" + boostSource.key().asString());

    String desc = boostSource.description();
    if (desc != null && !desc.isEmpty()) {
      Messages.send(sender, "<neutral>Description: <secondary>" + desc);
    }

    if (boostSource instanceof RuledBoostSource ruled) {
      showRuledSourceDetails(sender, ruled);
    }

    return Command.SINGLE_SUCCESS;
  }

  private void showRuledSourceDetails(CommandSender sender, RuledBoostSource source) {
    var rules = source.rules();
    Messages.send(sender, "<neutral>Rules: <secondary>" + rules.size() + " rule(s)");

    Messages.send(sender, "");

    for (int i = 0; i < rules.size(); i++) {
      Rule rule = rules.get(i);
      Messages.send(sender, "<secondary>  Rule #" + (i + 1));
      Messages.send(sender, "<neutral>    Priority: <secondary>" + rule.priority());
      Messages.send(sender, "<neutral>    Boost: <accent>" + formatBoost(rule.boost()));
      Messages.send(sender, "<neutral>    Condition:");

      List<String> tree = ConditionTreeFormatter.format(rule.condition(), "      ");
      for (String line : tree) {
        Messages.send(sender, "<accent>" + line);
      }

      if (i < rules.size() - 1) {
        Messages.send(sender, "");
      }
    }
  }

  private String formatBoostEffects(BoostSource source) {
    if (source instanceof RuledBoostSource ruledSource) {
      List<Rule> rules = ruledSource.rules();
      if (rules.isEmpty()) {
        return "No effects";
      }

      // Collect all unique boost effects
      List<String> effects = rules.stream()
          .map(rule -> formatBoost(rule.boost()))
          .distinct()
          .collect(Collectors.toList());

      if (effects.size() == 1) {
        return effects.get(0);
      }

      return String.join(", ", effects);
    }

    // For non-ruled boost sources, try to get description
    String desc = source.description();
    return desc != null && !desc.isEmpty() ? desc : "Active";
  }

  private String formatBoost(Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl m -> "x" + m.amount().stripTrailingZeros().toPlainString();
      case AdditiveBoostImpl a -> "+" + a.amount().stripTrailingZeros().toPlainString();
      default -> boost.getClass().getSimpleName();
    };
  }

  private record PassiveBoostInfo(BoostSource boostSource, int slot) {

  }
}
