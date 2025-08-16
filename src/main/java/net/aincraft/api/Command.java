package net.aincraft.api;

import net.aincraft.api.action.ActionTypes;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.Condition.Codec;
import net.aincraft.api.container.boost.Condition.Codec.Typed;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.CodecRegistry;
import net.aincraft.api.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {

    RegistryView<Codec> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.CONDITION_CODEC);
    if (registry instanceof CodecRegistry cr) {
      byte[] encoded = cr.encode(Condition.biome(Biome.BADLANDS).or(Condition.biome(Biome.PLAINS))
          .or(Condition.biome(Biome.BAMBOO_JUNGLE)));
      Bukkit.broadcastMessage("Encoded Base64: " + encoded.toString());
      Condition condition = cr.decode(encoded);
      Bukkit.broadcastMessage(condition.toString());
    }
    return false;
  }
}
