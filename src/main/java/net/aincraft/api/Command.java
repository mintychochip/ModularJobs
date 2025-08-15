package net.aincraft.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.aincraft.api.action.ActionTypes;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.ConditionAdapter;
import net.aincraft.api.container.boost.ConditionAdapter.Writer;
import net.aincraft.api.container.boost.ConditionCodecImpl;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.container.boost.RelationalOperator;
import net.aincraft.api.container.boost.conditions.BiomeCondition;
import net.aincraft.api.container.boost.conditions.PlayerResourceCondition;
import net.aincraft.api.container.boost.conditions.PlayerResourceCondition.PlayerResourceType;
import net.aincraft.api.container.boost.conditions.SneakCondition;
import net.aincraft.api.container.boost.conditions.TestAdapter;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.bridge.BridgeImpl;
import net.aincraft.container.JobProgressionImpl;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
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
    TestAdapter adapter = new TestAdapter();
    Out out = new Out(16);
    ConditionCodecImpl codec = new ConditionCodecImpl();
    codec.register(new TestAdapter());
    byte[] encode = codec.encode(Condition.biome(Biome.BADLANDS));
    Bukkit.broadcastMessage(encode + "");
    Condition condition = codec.decode(encode);
    RegistryView<Job> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.JOBS);
    Job job = registry.getOrThrow(Key.key("jobs:builder"));
    Bukkit.broadcastMessage(
        condition.applies(
            new BoostContext(ActionTypes.SHEAR, ProgressionService.progressionService()
                .get((Player) sender, job), (Player) sender)) + "");
    return false;
  }
}
