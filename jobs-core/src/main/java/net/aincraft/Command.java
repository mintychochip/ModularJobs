package net.aincraft;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.boost.policy.AllApplicablePolicyImpl;
import net.aincraft.container.boost.BoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.serialization.CodecRegistry;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

  private final JobService jobService;
  private final CodecRegistry codecRegistry;
  private final ItemBoostDataService boostDataService;

  @Inject
  public Command(JobService jobService, CodecRegistry codecRegistry,
      ItemBoostDataService boostDataService) {
    this.jobService = jobService;
    this.codecRegistry = codecRegistry;
    this.boostDataService = boostDataService;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender,
      org.bukkit.command.Command command, @NotNull String label,
      @NotNull String @NotNull [] args) {
    Rule rule = new Rule(new BiomeConditionImpl(Biome.PLAINS.key()), 0,
        new AdditiveBoostImpl(BigDecimal.valueOf(1.0)));
    RuledBoostSourceImpl source = new RuledBoostSourceImpl(List.of(rule),
        AllApplicablePolicyImpl.INSTANCE);
    ItemStack stack = ItemStack.of(
        Material.COOKED_BEEF);
    boostDataService.addData(new ConsumableBoostData(source, Duration.ofMinutes(5)), stack);
    if (sender instanceof Player player) {
      player.getInventory().addItem(stack);
    }
    return false;
  }
}
