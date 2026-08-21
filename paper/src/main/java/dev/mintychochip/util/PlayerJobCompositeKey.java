package dev.mintychochip.util;

import java.util.UUID;
import dev.mintychochip.Job;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;

/**
 * Compound identity for a player and a job.
 *
 * @param playerId player UUID
 * @param jobKey namespaced job key
 */
public record PlayerJobCompositeKey(UUID playerId, Key jobKey) {

  /**
   * Creates a key from an offline player and a job.
   *
   * @param player player whose UUID is used
   * @param job job whose namespaced key is used
   * @return compound key
   */
  public static PlayerJobCompositeKey create(OfflinePlayer player, Job job) {
    return new PlayerJobCompositeKey(player.getUniqueId(), job.key());
  }
}
