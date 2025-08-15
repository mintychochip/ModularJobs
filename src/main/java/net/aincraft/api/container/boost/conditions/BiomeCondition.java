package net.aincraft.api.container.boost.conditions;

import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public record BiomeCondition(NamespacedKey biomeKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    Location location = player.getLocation();
    World world = location.getWorld();
    return biomeKey.equals(world.getBiome(location).getKey());
  }
}
