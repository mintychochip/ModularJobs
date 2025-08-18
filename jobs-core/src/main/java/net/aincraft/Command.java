package net.aincraft;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {
//
//    RegistryView<Codec> registry = RegistryContainer.registryContainer()
//        .getRegistry(RegistryKeys.CODEC);
//    if (registry instanceof CodecRegistry cr && sender instanceof Player player) {
//      PlayerInventory inventory = player.getInventory();
//      ItemBoostDataServiceImpl itemBoostDataService = new ItemBoostDataServiceImpl(cr);
//      ItemStack stack = ItemStack.of(Material.STONE);
//      RuledBoostSourceImpl boostSource = new RuledBoostSourceImpl(
//          List.of(new Rule(Condition.biome(
//              Biome.BAMBOO_JUNGLE), 0, new AdditiveBoostImpl(BigDecimal.TEN))),
//          PolicyFactory.policyFactory().allApplicable());
//      SlotSetImpl slotSet = new SlotSetImpl(0x01FF);
//      ConsumableBoostData boostData = new ConsumableBoostData(boostSource,
//          Duration.ofMinutes(5));
//      Bridge.bridge().timedBoostDataService().addData(boostData, new PlayerTarget(player));
//      List<ActiveBoostData> boosts = Bridge.bridge().timedBoostDataService()
//          .findBoosts(new PlayerTarget(player));
//      BoostEngineImpl engine = new BoostEngineImpl(itemBoostDataService,
//          Bridge.bridge().timedBoostDataService());
//      Job job = RegistryContainer.registryContainer().getRegistry(RegistryKeys.JOBS)
//          .getOrThrow(Key.key("jobs:builder"));
//      List<Boost> boostList = engine.evaluate(ActionTypes.BLOCK_BREAK,
//          ProgressionService.progressionService().get(player, job), player);
//    }
    return false;
  }
}
