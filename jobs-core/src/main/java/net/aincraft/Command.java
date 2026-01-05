package net.aincraft;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.key.Key;
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
import net.aincraft.upgrade.JobUpgradeNode;
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
    // For this factory, we'll return all defined nodes.
    JobUpgradeNode root = buildSampleGraph();
    Rule rule = new Rule(new BiomeConditionImpl(Biome.PLAINS.key()), 0,
        new AdditiveBoostImpl(BigDecimal.valueOf(1.0)));
    ItemStack stack = ItemStack.of(
        Material.COOKED_BEEF);
    if (sender instanceof Player player) {
      player.getInventory().addItem(stack);
    }
    return false;
  }
  public static JobUpgradeNode buildSampleGraph() {
    // 1. Create the final destination node (Node D - no neighbors)
    JobUpgradeNode masteryTier = new JobUpgradeNode(
        "Mastery Tier",
        5,
        Collections.emptyList() // No neighbors
    );

    // 2. Create intermediate nodes (Node B and Node C), both leading to Mastery Tier (D)
    JobUpgradeNode skillPath1 = new JobUpgradeNode(
        "Skill Path 1",
        10,
        Arrays.asList(masteryTier) // Neighbor: D
    );

    JobUpgradeNode skillPath2 = new JobUpgradeNode(
        "Skill Path 2",
        8,
        Arrays.asList(masteryTier) // Neighbor: D
    );

    // 3. Create the starting node (Node A), which leads to the two paths (B and C)
    JobUpgradeNode startingJob = new JobUpgradeNode(
        "Starting Job",
        0,
        Arrays.asList(skillPath1, skillPath2) // Neighbors: B and C
    );

    return startingJob;
  }
}
