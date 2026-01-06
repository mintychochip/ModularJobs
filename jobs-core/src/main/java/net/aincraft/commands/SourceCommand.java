package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.boost.conditions.ComposableConditionImpl;
import net.aincraft.boost.conditions.LiquidConditionImpl;
import net.aincraft.boost.conditions.NegatingConditionImpl;
import net.aincraft.boost.conditions.PlayerResourceConditionImpl;
import net.aincraft.boost.conditions.PotionConditionImpl;
import net.aincraft.boost.conditions.PotionTypeConditionImpl;
import net.aincraft.boost.conditions.SneakConditionImpl;
import net.aincraft.boost.conditions.SprintConditionImpl;
import net.aincraft.boost.conditions.WeatherConditionImpl;
import net.aincraft.boost.conditions.WorldConditionImpl;
import net.aincraft.boost.config.BoostSourceLoader;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Generic boost source management command.
 */
public final class SourceCommand implements JobsCommand {

  private final Registry<BoostSource> boostSourceRegistry;
  private final BoostSourceLoader boostSourceLoader;

  @Inject
  public SourceCommand(
      Registry<BoostSource> boostSourceRegistry,
      BoostSourceLoader boostSourceLoader
  ) {
    this.boostSourceRegistry = boostSourceRegistry;
    this.boostSourceLoader = boostSourceLoader;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("source")
        .then(Commands.literal("list")
            .executes(context -> listBoostSources(context.getSource()))
        )
        .then(Commands.literal("info")
            .then(Commands.argument("boostKey", ArgumentTypes.key())
                .suggests((context, builder) -> {
                  boostSourceRegistry.stream()
                      .map(source -> source.key().asString())
                      .forEach(builder::suggest);
                  return builder.buildFuture();
                })
                .executes(context -> {
                  return showBoostSourceInfo(
                      context.getSource(),
                      context.getArgument("boostKey", Key.class).asString()
                  );
                })
            )
        )
        .then(Commands.literal("reload")
            .executes(context -> reloadBoostSources(context.getSource()))
        );
  }

  private int listBoostSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();

    var sources = boostSourceRegistry.stream().collect(Collectors.toList());

    if (sources.isEmpty()) {
      Mint.sendMessage(sender, "<secondary>No boost sources registered");
      return 0;
    }

    Mint.sendMessage(sender, "<neutral>━━━━ <primary>Boost Sources <neutral>━━━━");

    for (BoostSource boostSource : sources) {
      Mint.sendMessage(sender, "<neutral>  • <secondary>" + boostSource.key().asString());

      // Add description inline if available
      String description = boostSource.description();
      if (description != null && !description.isEmpty()) {
        Mint.sendMessage(sender, "<neutral>    " + description);
      }
    }

    Mint.sendMessage(sender, "<neutral>Total: " + sources.size());

    return Command.SINGLE_SUCCESS;
  }

  private int showBoostSourceInfo(CommandSourceStack source, String boostKeyStr) {
    CommandSender sender = source.getSender();

    Key boostKey = Key.key(boostKeyStr);
    BoostSource boostSource = boostSourceRegistry.get(boostKey).orElse(null);

    if (boostSource == null) {
      Mint.sendMessage(sender, "<error>Boost source not found: " + boostKeyStr);
      return 0;
    }

    Mint.sendMessage(sender, "<neutral>━━━━ <primary>Boost Source Info <neutral>━━━━");

    Mint.sendMessage(sender, "<neutral>Key: <secondary>" + boostSource.key().asString());

    // Show description if available
    String description = boostSource.description();
    if (description != null && !description.isEmpty()) {
      Mint.sendMessage(sender, "<neutral>Description: <secondary>" + description);
    }

    Mint.sendMessage(sender, "<neutral>Type: <accent>" + boostSource.getClass().getSimpleName());

    // Show detailed info for RuledBoostSource
    if (boostSource instanceof RuledBoostSource ruledSource) {
      showRuledBoostSourceDetails(sender, ruledSource);
    }

    return Command.SINGLE_SUCCESS;
  }

  private void showRuledBoostSourceDetails(CommandSender sender, RuledBoostSource source) {
    // Policy info
    RuledBoostSource.Policy policy = source.policy();
    String policyName = policy.getClass().getSimpleName()
        .replace("Impl", "")
        .replace("Policy", "");
    Mint.sendMessage(sender, "<neutral>Policy: <info>" + policyName);

    // Rules info
    var rules = source.rules();
    Mint.sendMessage(sender, "<neutral>Rules: <secondary>" + rules.size() + " rule(s)");

    sender.sendMessage(Component.empty());

    // List each rule
    for (int i = 0; i < rules.size(); i++) {
      Rule rule = rules.get(i);
      Mint.sendMessage(sender, "<secondary>  Rule #" + (i + 1));

      Mint.sendMessage(sender, "<neutral>    Priority: <secondary>" + rule.priority());

      // Boost info
      Boost boost = rule.boost();
      String boostInfo = formatBoost(boost);
      Mint.sendMessage(sender, "<neutral>    Boost: <accent>" + boostInfo);

      // Condition tree
      Mint.sendMessage(sender, "<neutral>    Condition:");
      List<String> conditionTree = formatConditionTree(rule.condition(), "      ");
      for (String line : conditionTree) {
        Mint.sendMessage(sender, "<accent>" + line);
      }

      if (i < rules.size() - 1) {
        sender.sendMessage(Component.empty());
      }
    }
  }

  private List<String> formatConditionTree(Condition condition, String indent) {
    List<String> lines = new ArrayList<>();
    formatConditionTreeRecursive(condition, indent, "", true, lines);
    return lines;
  }

  private void formatConditionTreeRecursive(Condition condition, String baseIndent, String prefix, boolean isLast, List<String> lines) {
    String connector = isLast ? "└── " : "├── ";
    String childPrefix = isLast ? "    " : "│   ";

    if (condition instanceof ComposableConditionImpl composite) {
      String operator = composite.logicalOperator().name();
      lines.add(baseIndent + prefix + connector + operator);
      formatConditionTreeRecursive(composite.a(), baseIndent, prefix + childPrefix, false, lines);
      formatConditionTreeRecursive(composite.b(), baseIndent, prefix + childPrefix, true, lines);

    } else if (condition instanceof NegatingConditionImpl negated) {
      lines.add(baseIndent + prefix + connector + "NOT");
      formatConditionTreeRecursive(negated.condition(), baseIndent, prefix + childPrefix, true, lines);

    } else if (condition instanceof BiomeConditionImpl biome) {
      lines.add(baseIndent + prefix + connector + "Biome: " + biome.biomeKey().value());

    } else if (condition instanceof WorldConditionImpl world) {
      lines.add(baseIndent + prefix + connector + "World: " + world.worldKey().value());

    } else if (condition instanceof SneakConditionImpl sneak) {
      lines.add(baseIndent + prefix + connector + "Sneaking: " + sneak.state());

    } else if (condition instanceof SprintConditionImpl sprint) {
      lines.add(baseIndent + prefix + connector + "Sprinting: " + sprint.state());

    } else if (condition instanceof PlayerResourceConditionImpl resource) {
      String opSymbol = switch (resource.operator()) {
        case LESS_THAN -> "<";
        case LESS_THAN_OR_EQUAL -> "<=";
        case GREATER_THAN -> ">";
        case GREATER_THAN_OR_EQUAL -> ">=";
        case EQUAL -> "==";
        case NOT_EQUAL -> "!=";
      };
      lines.add(baseIndent + prefix + connector + resource.type() + " " + opSymbol + " " + resource.expected());

    } else if (condition instanceof PotionConditionImpl potion) {
      String opSymbol = switch (potion.relationalOperator()) {
        case LESS_THAN -> "<";
        case LESS_THAN_OR_EQUAL -> "<=";
        case GREATER_THAN -> ">";
        case GREATER_THAN_OR_EQUAL -> ">=";
        case EQUAL -> "==";
        case NOT_EQUAL -> "!=";
      };
      lines.add(baseIndent + prefix + connector + "Potion: " + potion.type().key().value() + " " + potion.conditionType() + " " + opSymbol + " " + potion.expected());

    } else if (condition instanceof PotionTypeConditionImpl potionType) {
      lines.add(baseIndent + prefix + connector + "Has Potion: " + potionType.type().key().value());

    } else if (condition instanceof WeatherConditionImpl weather) {
      lines.add(baseIndent + prefix + connector + "Weather: " + weather.state());

    } else if (condition instanceof LiquidConditionImpl liquid) {
      lines.add(baseIndent + prefix + connector + "In Liquid: " + liquid.liquid().name().toLowerCase());

    } else {
      // Fallback for unknown condition types
      String type = condition.getClass().getSimpleName().replace("Impl", "").replace("Condition", "");
      lines.add(baseIndent + prefix + connector + type);
    }
  }

  private String formatBoost(Boost boost) {
    if (boost instanceof MultiplicativeBoostImpl multi) {
      BigDecimal amount = multi.amount();
      return "×" + amount + " (multiplicative)";
    } else if (boost instanceof AdditiveBoostImpl add) {
      BigDecimal amount = add.amount();
      return "+" + amount + " (additive)";
    } else {
      return boost.getClass().getSimpleName();
    }
  }

  private int reloadBoostSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();

    Mint.sendMessage(sender, "<secondary>Reloading boost sources...");

    int count = boostSourceLoader.reload();

    Mint.sendMessage(sender, "<accent>Reloaded <primary>" + count + "<accent> boost source(s)");

    return Command.SINGLE_SUCCESS;
  }
}
