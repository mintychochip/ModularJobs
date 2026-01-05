package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.util.stream.Collectors;
import net.aincraft.boost.config.BoostSourceLoader;
import net.aincraft.container.BoostSource;
import net.aincraft.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
      sender.sendMessage(Component.text("No boost sources registered", NamedTextColor.YELLOW));
      return 0;
    }

    sender.sendMessage(
        Component.text("━━━━ ", NamedTextColor.GRAY)
            .append(Component.text("Boost Sources", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(" ━━━━", NamedTextColor.GRAY))
    );

    for (BoostSource boostSource : sources) {
      Component line = Component.text("  • ", NamedTextColor.GRAY)
          .append(Component.text(boostSource.key().asString(), NamedTextColor.YELLOW));

      // Add description inline if available
      String description = boostSource.description();
      if (description != null && !description.isEmpty()) {
        line = line.append(Component.newline())
            .append(Component.text("    ", NamedTextColor.GRAY))
            .append(Component.text(description, NamedTextColor.DARK_GRAY));
      }

      sender.sendMessage(line);
    }

    sender.sendMessage(
        Component.text("Total: " + sources.size(), NamedTextColor.GRAY)
    );

    return Command.SINGLE_SUCCESS;
  }

  private int showBoostSourceInfo(CommandSourceStack source, String boostKeyStr) {
    CommandSender sender = source.getSender();

    Key boostKey = Key.key(boostKeyStr);
    BoostSource boostSource = boostSourceRegistry.get(boostKey).orElse(null);

    if (boostSource == null) {
      sender.sendMessage(Component.text("Boost source not found: " + boostKeyStr, NamedTextColor.RED));
      return 0;
    }

    sender.sendMessage(
        Component.text("━━━━ ", NamedTextColor.GRAY)
            .append(Component.text("Boost Source Info", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(" ━━━━", NamedTextColor.GRAY))
    );

    sender.sendMessage(
        Component.text("Key: ", NamedTextColor.GRAY)
            .append(Component.text(boostSource.key().asString(), NamedTextColor.YELLOW))
    );

    // Show description if available
    String description = boostSource.description();
    if (description != null && !description.isEmpty()) {
      sender.sendMessage(
          Component.text("Description: ", NamedTextColor.GRAY)
              .append(Component.text(description, NamedTextColor.WHITE))
      );
    }

    sender.sendMessage(
        Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(boostSource.getClass().getSimpleName(), NamedTextColor.AQUA))
    );

    // TODO: Add more detailed info for RuledBoostSource (rules, policy, etc.)

    return Command.SINGLE_SUCCESS;
  }

  private int reloadBoostSources(CommandSourceStack source) {
    CommandSender sender = source.getSender();

    sender.sendMessage(Component.text("Reloading boost sources...", NamedTextColor.YELLOW));

    int count = boostSourceLoader.reload();

    sender.sendMessage(
        Component.text("Reloaded ", NamedTextColor.GREEN)
            .append(Component.text(count, NamedTextColor.GOLD))
            .append(Component.text(" boost source(s)", NamedTextColor.GREEN))
    );

    return Command.SINGLE_SUCCESS;
  }
}
