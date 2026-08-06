package net.aincraft.payment;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.util.LocationKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Jobs Reborn hopper fill-up protection: blocks that received items via hopper lose job pay
 * attribution until cleared (player re-opens / ownership reassert — we clear on player interact).
 */
final class HopperPayDisableStore {

  private final Set<LocationKey> disabled = ConcurrentHashMap.newKeySet();

  void disable(@NotNull Block block) {
    disabled.add(LocationKey.create(block.getLocation()));
  }

  void clear(@NotNull Block block) {
    disabled.remove(LocationKey.create(block.getLocation()));
  }

  boolean isDisabled(@NotNull Block block) {
    return disabled.contains(LocationKey.create(block.getLocation()));
  }
}
