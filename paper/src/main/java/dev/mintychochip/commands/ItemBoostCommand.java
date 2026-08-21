package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.BitSet;
import dev.mintychochip.boost.SlotSetParser;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Item boost command for giving items with boost sources attached.
 */
public final class ItemBoostCommand implements JobsCommand {

  private final Registry<BoostSource> boostSourceRegistry;
  private final ItemBoostDataService itemBoostDataService;

  public ItemBoostCommand(
      Registry<BoostSource> boostSourceRegistry,
      ItemBoostDataService itemBoostDataService
  ) {
    this.boostSourceRegistry = boostSourceRegistry;
    this.itemBoostDataService = itemBoostDataService;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("itemboost")
        .then(buildGiveCommand());
  }

  private LiteralArgumentBuilder<CommandSourceStack> buildGiveCommand() {
    return Commands.literal("give")
        .then(Commands.argument("player", ArgumentTypes.player())
            .then(Commands.argument("material", StringArgumentType.word())
                .then(Commands.argument("boostKey", ArgumentTypes.key())
                    .suggests((context, builder) -> {
                      boostSourceRegistry.stream()
                          .map(source -> source.key().asString())
                          .forEach(builder::suggest);
                      return builder.buildFuture();
                    })
                    .executes(context -> {
                      Player target = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                          .resolve(context.getSource()).getFirst();
                      return giveItemBoost(
                          context.getSource(),
                          target,
                          context.getArgument("material", String.class),
                          context.getArgument("boostKey", Key.class).asString(),
                          "all" // default slot set
                      );
                    })
                    .then(Commands.argument("slotSet", StringArgumentType.greedyString())
                        .executes(context -> {
                          Player target = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                              .resolve(context.getSource()).getFirst();
                          return giveItemBoost(
                              context.getSource(),
                              target,
                              context.getArgument("material", String.class),
                              context.getArgument("boostKey", Key.class).asString(),
                              context.getArgument("slotSet", String.class)
                          );
                        })
                    )
                )
            )
        );
  }

  private int giveItemBoost(
      CommandSourceStack source,
      Player target,
      String materialName,
      String boostKeyStr,
      String slotSetSpec
  ) {
    CommandSender sender = source.getSender();

    // Parse material
    Material material;
    try {
      material = Material.valueOf(materialName.toUpperCase());
    } catch (IllegalArgumentException e) {
      Messages.send(sender, "<error>Invalid material: " + materialName);
      return 0;
    }

    // Get boost source
    Key boostKey = Key.key(boostKeyStr);
    BoostSource boostSource = boostSourceRegistry.get(boostKey).orElse(null);
    if (boostSource == null) {
      Messages.send(sender, "<error>Boost source not found: " + boostKeyStr);
      return 0;
    }

    // Parse slot set
    BitSet slotSet;
    try {
      slotSet = SlotSetParser.parse(slotSetSpec);
    } catch (IllegalArgumentException e) {
      Messages.send(sender, "<error>Invalid slot specification: " + e.getMessage());
      return 0;
    }

    // Create item with boost data
    ItemStack item = new ItemStack(material);
    PassiveBoostData boostData = new PassiveBoostData(boostSource, slotSet);
    itemBoostDataService.addData(boostData, item);

    // Add lore
    item.lore(java.util.List.of(
        Component.text("Boost: " + boostKeyStr),
        Component.text("Slots: " + slotSetSpec)
    ));

    // Give to player
    target.getInventory().addItem(item);

    Messages.send(sender, "<accent>Gave <secondary>" + target.getName() + "<accent> an item with boost <primary>" + boostKeyStr);

    return Command.SINGLE_SUCCESS;
  }

}
