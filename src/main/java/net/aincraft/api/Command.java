package net.aincraft.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.bridge.BridgeImpl;
import net.aincraft.container.JobProgressionImpl;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {
    if (sender instanceof Player player) {
      RegistryView<Job> registry = RegistryContainer.registryContainer()
          .getRegistry(RegistryKeys.JOBS);
      if (registry.isRegistered(Key.key("jobs:builder"))) {
        Job job = registry.getOrThrow(Key.key("jobs:builder"));
        Instant now = Instant.now();
        JobProgression progression = ProgressionService.progressionService().get(player, job);
        if (progression == null) {
          return false;
        }
        for (int i = 0; i <= 10000000; ++i) {
          progression = progression.addExperience(BigDecimal.TEN);
        }
        ProgressionService.progressionService().update(progression);
        Boost boost = Boost.additive(BoostType.PLAYER, 15);
        boost.andThen(Boost.additive(BoostType.PLAYER,15));
        Bukkit.broadcastMessage(Duration.between(now,Instant.now()).toString());
      }
    }
    return false;
  }
}
