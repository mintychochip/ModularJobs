package dev.mintychochip.payment;

import dev.mintychochip.util.LocationKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Blocks that received items via hopper lose job pay attribution until a player reopens the
 * container and ownership is reasserted.
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
