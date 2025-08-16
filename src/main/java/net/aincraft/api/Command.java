package net.aincraft.api;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.api.action.ActionTypes;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.PolicyFactory;
import net.aincraft.api.container.RuledBoostSource.Rule;
import net.aincraft.api.container.Codec.Typed;
import net.aincraft.api.container.RuledBoostSourceImpl;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.container.boost.PotionConditionType;
import net.aincraft.api.container.boost.RelationalOperator;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.CodecRegistry;
import net.aincraft.api.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
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
      Rule rule = cr.decode(encoded, Rule.class);
      JobProgression progression = ProgressionService.progressionService()
          .get((Player) sender, builder);
      BoostContext contxt = new BoostContext(ActionTypes.WAX, progression, (Player) sender);
      RuledBoostSourceImpl source = new RuledBoostSourceImpl(
          PolicyFactory.policyFactory().getFirst(), List.of(new Rule(condition, 2,
          Boost.additive(BoostType.MCMMO,
              BigDecimal.TEN))));
      List<Boost> boosts = source.evaluate(contxt);
      Bukkit.broadcastMessage(boosts.toString());
    }
    return false;
  }
}
