package net.aincraft;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.key.Key;
import me.clip.placeholderapi.PlaceholderAPI;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.serialization.CodecRegistry;
import net.aincraft.service.JobService;
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
    // Test command - gives player a cooked beef item
    Rule rule = new Rule(new BiomeConditionImpl(Biome.PLAINS.key()), 0,
        new AdditiveBoostImpl(BigDecimal.valueOf(1.0)));
    ItemStack stack = ItemStack.of(Material.COOKED_BEEF);
    if (sender instanceof Player player) {
      player.getInventory().addItem(stack);
    }
    return false;
  }
}
