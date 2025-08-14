package net.aincraft.util;

import com.google.common.cache.Cache;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.aincraft.api.Job;
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
