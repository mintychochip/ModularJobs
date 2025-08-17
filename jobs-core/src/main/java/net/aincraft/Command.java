package net.aincraft;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.BoostEngineImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.SerializableBoostDataImpl;
import net.aincraft.container.ActionTypes;
import net.aincraft.container.Boost;
import net.aincraft.container.SlotSetImpl;
import net.aincraft.serialization.Codec;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.aincraft.service.CodecRegistry;
import net.aincraft.service.ItemBoostDataServiceImpl;
import net.aincraft.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {

    RegistryView<Codec> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.CODEC);
    if (registry instanceof CodecRegistry cr && sender instanceof Player player) {
      PlayerInventory inventory = player.getInventory();
      ItemBoostDataServiceImpl itemBoostDataService = new ItemBoostDataServiceImpl(cr);
      ItemStack stack = ItemStack.of(Material.STONE);
      RuledBoostSourceImpl boostSource = new RuledBoostSourceImpl(
          List.of(new Rule(Condition.biome(
              Biome.BAMBOO_JUNGLE), 0, new AdditiveBoostImpl(BigDecimal.TEN))),
          PolicyFactory.policyFactory().allApplicable());
      SlotSetImpl slotSet = new SlotSetImpl(0x01FF);
      itemBoostDataService.addData(
          new SerializableBoostDataImpl(boostSource, slotSet, null), stack);
      inventory.addItem(stack);
      BoostEngineImpl engine = new BoostEngineImpl(itemBoostDataService);
      Job job = RegistryContainer.registryContainer().getRegistry(RegistryKeys.JOBS)
          .getOrThrow(Key.key("jobs:builder"));
      List<Boost> boosts = engine.evaluate(ActionTypes.BLOCK_PLACE,
          ProgressionService.progressionService().get(player, job), player);
      Bukkit.broadcastMessage(boosts.toString());
    }
    return false;
  }
}
