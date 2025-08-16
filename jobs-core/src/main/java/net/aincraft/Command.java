package net.aincraft;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.container.ActionTypes;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.BoostType;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.aincraft.service.CodecRegistry;
import net.aincraft.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {

    RegistryView<Codec> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.CODEC);
    RegistryView<Job> job = RegistryContainer.registryContainer().getRegistry(RegistryKeys.JOBS);
    Job builder = job.getOrThrow(Key.key("jobs:builder"));
    if (registry instanceof CodecRegistry cr) {
      Condition condition = Condition.liquid(Material.WATER)
          .and(Condition.potion(PotionEffectType.STRENGTH, 2,
              PotionConditionType.AMPLIFIER, RelationalOperator.GREATER_THAN_OR_EQUAL)).and(
              Condition.potion(PotionEffectType.STRENGTH, 600, PotionConditionType.DURATION,
                  RelationalOperator.GREATER_THAN));
      byte[] encoded = cr.encode(new Rule(condition, 2,
          Boost.additive(BoostType.MCMMO,
              BigDecimal.TEN)));
      JobProgression progression = ProgressionService.progressionService()
          .get((Player) sender, builder);
      RuledBoostSourceImpl source = new RuledBoostSourceImpl(
          PolicyFactory.policyFactory().getFirst(), List.of(new Rule(condition, 2,
          Boost.additive(BoostType.MCMMO,
              BigDecimal.TEN))));
      BoostContext contxt = new BoostContext(ActionTypes.WAX, progression,
          ((Player) sender).getPlayer());
      List<Boost> boosts = source.evaluate(contxt);
      Bukkit.broadcastMessage(boosts.toString());
    }
    return false;
  }
}
