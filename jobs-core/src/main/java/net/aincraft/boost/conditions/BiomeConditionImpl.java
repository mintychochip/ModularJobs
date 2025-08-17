package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public record BiomeConditionImpl(Key biomeKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    Location location = player.getLocation();
    World world = location.getWorld();
    return biomeKey.equals(world.getBiome(location).getKey());
  }

}
