package net.aincraft;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.inject.Inject;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.boost.conditions.ComposableConditionImpl;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.units.qual.K;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  private final JobService jobService;

  @Inject
  public Command(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.@NotNull Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {
    return false;
  }
}
