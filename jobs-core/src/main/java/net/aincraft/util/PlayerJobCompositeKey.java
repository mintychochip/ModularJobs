package net.aincraft.util;

import java.util.Objects;
import java.util.UUID;
import net.aincraft.Job;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;

public record PlayerJobCompositeKey(UUID playerId, Key jobKey) {

  @Override
  public int hashCode() {
    return Objects.hash(playerId,jobKey);
  }

  public static PlayerJobCompositeKey create(OfflinePlayer player, Job job) {
    return new PlayerJobCompositeKey(player.getUniqueId(), job.key());
  }
}
