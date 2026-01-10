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
      Mint.sendThemedMessage(sender, "<secondary>No boost sources registered");
      return 0;
    }

    Mint.sendThemedMessage(sender, "<neutral>━━━━ <primary>Boost Sources <neutral>━━━━");

    for (BoostSource boostSource : sources) {
      Mint.sendThemedMessage(sender, "<neutral>  • <secondary>" + boostSource.key().asString());

      // Add description inline if available
      String description = boostSource.description();
      if (description != null && !description.isEmpty()) {
        Mint.sendThemedMessage(sender, "<neutral>    " + description);
      }
    }

    Mint.sendThemedMessage(sender, "<neutral>Total: " + sources.size());

    return Command.SINGLE_SUCCESS;
  }

  private int showBoostSourceInfo(CommandSourceStack source, String boostKeyStr) {
    CommandSender sender = source.getSender();

    Key boostKey = Key.key(boostKeyStr);
    BoostSource boostSource = boostSourceRegistry.get(boostKey).orElse(null);

    if (boostSource == null) {
      Mint.sendThemedMessage(sender, "<error>Boost source not found: " + boostKeyStr);
      return 0;
    }

    Mint.sendThemedMessage(sender, "<neutral>━━━━ <primary>Boost Source Info <neutral>━━━━");

    Mint.sendThemedMessage(sender, "<neutral>Key: <secondary>" + boostSource.key().asString());

    // Show description if available
    String description = boostSource.description();
    if (description != null && !description.isEmpty()) {
      Mint.sendThemedMessage(sender, "<neutral>Description: <secondary>" + description);
    }

    Mint.sendThemedMessage(sender, "<neutral>Type: <accent>" + boostSource.getClass().getSimpleName());

    // Show detailed info for RuledBoostSource
    if (boostSource instanceof RuledBoostSource ruledSource) {
      showRuledBoostSourceDetails(sender, ruledSource);
    }

    return Command.SINGLE_SUCCESS;
  }

  private void showRuledBoostSourceDetails(CommandSender sender, RuledBoostSource source) {
    // Rules info
    var rules = source.rules();
    Mint.sendThemedMessage(sender, "<neutral>Rules: <secondary>" + rules.size() + " rule(s)");

    sender.sendMessage(Component.empty());

    // List each rule
    for (int i = 0; i < rules.size(); i++) {
      Rule rule = rules.get(i);
      Mint.sendThemedMessage(sender, "<secondary>  Rule #" + (i + 1));

      Mint.sendThemedMessage(sender, "<neutral>    Priority: <secondary>" + rule.priority());

      // Boost info
      Boost boost = rule.boost();
      String boostInfo = formatBoost(boost);
      Mint.sendThemedMessage(sender, "<neutral>    Boost: <accent>" + boostInfo);

      // Condition tree
      Mint.sendThemedMessage(sender, "<neutral>    Condition:");
      List<String> conditionTree = formatConditionTree(rule.condition(), "      ");
      for (String line : conditionTree) {
        Mint.sendThemedMessage(sender, "<accent>" + line);
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

    switch (condition) {
      case ComposableConditionImpl composite -> {
        lines.add(baseIndent + prefix + connector + composite.logicalOperator().name());
        formatConditionTreeRecursive(composite.a(), baseIndent, prefix + childPrefix, false, lines);
        formatConditionTreeRecursive(composite.b(), baseIndent, prefix + childPrefix, true, lines);
      }
      case NegatingConditionImpl negated -> {
        lines.add(baseIndent + prefix + connector + "NOT");
        formatConditionTreeRecursive(negated.condition(), baseIndent, prefix + childPrefix, true, lines);
      }
      case BiomeConditionImpl b -> lines.add(baseIndent + prefix + connector + "Biome: " + b.biomeKey().value());
      case WorldConditionImpl w -> lines.add(baseIndent + prefix + connector + "World: " + w.worldKey().value());
      case SneakConditionImpl s -> lines.add(baseIndent + prefix + connector + "Sneaking: " + s.state());
      case SprintConditionImpl s -> lines.add(baseIndent + prefix + connector + "Sprinting: " + s.state());
      case PlayerResourceConditionImpl r -> lines.add(baseIndent + prefix + connector + r.type() + " " + formatOperator(r.operator()) + " " + r.expected());
      case PotionConditionImpl p -> lines.add(baseIndent + prefix + connector + "Potion: " + p.type().key().value() + " " + p.conditionType() + " " + formatOperator(p.relationalOperator()) + " " + p.expected());
      case PotionTypeConditionImpl p -> lines.add(baseIndent + prefix + connector + "Has Potion: " + p.type().key().value());
      case WeatherConditionImpl w -> lines.add(baseIndent + prefix + connector + "Weather: " + w.state());
      case LiquidConditionImpl l -> lines.add(baseIndent + prefix + connector + "In Liquid: " + l.liquid().name().toLowerCase());
      default -> lines.add(baseIndent + prefix + connector + condition.getClass().getSimpleName().replace("Impl", "").replace("Condition", ""));
    }
  }

  private String formatOperator(net.aincraft.container.boost.RelationalOperator op) {
    return switch (op) {
      case LESS_THAN -> "<";
      case LESS_THAN_OR_EQUAL -> "<=";
      case GREATER_THAN -> ">";
      case GREATER_THAN_OR_EQUAL -> ">=";
      case EQUAL -> "==";
      case NOT_EQUAL -> "!=";
    };
  }

  private String formatBoost(Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl m -> "×" + m.amount() + " (multiplicative)";
      case AdditiveBoostImpl a -> "+" + a.amount() + " (additive)";
      default -> boost.getClass().getSimpleName();
    };
  }

  private int reloadBoostSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();

    Mint.sendThemedMessage(sender, "<secondary>Reloading boost sources...");

    int count = boostSourceLoader.reload();

    Mint.sendThemedMessage(sender, "<accent>Reloaded <primary>" + count + "<accent> boost source(s)");

    return Command.SINGLE_SUCCESS;
  }
}
