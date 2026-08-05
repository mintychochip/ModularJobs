package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.kyori.adventure.key.Key;
import org.bukkit.World;

/**
 * Matches the player's world by namespaced key and/or plain world name so
 * config can use values like {@code world_nether} without requiring the world
 * to exist at parse time.
 */
public record WorldConditionImpl(Key worldKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    World world = context.world();
    if (world == null) {
      return false;
    }
    if (worldKey.equals(world.getKey())) {
      return true;
    }
    String name = world.getName();
    return name.equalsIgnoreCase(worldKey.value())
        || name.equalsIgnoreCase(worldKey.asString())
        || world.getKey().value().equalsIgnoreCase(worldKey.value());
  }
}
