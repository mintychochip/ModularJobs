package net.aincraft.api;

import java.math.BigDecimal;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.RuledBoostSource.Rule;
import net.aincraft.api.container.Codec.Typed;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.CodecRegistry;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {

    RegistryView<Codec> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.CODEC);
    RegistryContainer.registryContainer().editRegistry(RegistryKeys.CODEC, r -> r.register(
        new Typed<Rule>() {
          @Override
          public @NotNull Key key() {
            return Key.key("jobs:rule");
          }

          @Override
          public Class<?> type() {
            return Rule.class;
          }

          @Override
          public void encode(Out out, Rule object, Writer writer) {
            out.writeKey();
          }

          @Override
          public Object decode(In in, Reader reader) {
            return null;
          }
        }));
    if (registry instanceof CodecRegistry cr) {
      byte[] encoded = cr.encode(new Rule(Condition.biome(Biome.BADLANDS)
          .or(Condition.biome(Biome.PLAINS))
          .or(Condition.biome(Biome.BAMBOO_JUNGLE)), 2, Boost.additive(BoostType.MCMMO,
          BigDecimal.TEN)));
      Bukkit.broadcastMessage(encoded.toString());
    }
    return false;
  }
}
