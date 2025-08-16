package net.aincraft.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record LocationKey(@NotNull String worldName, int x, int y, int z) {

  public static final LoadingCache<Location, LocationKey> CACHE =
      CacheBuilder.newBuilder()
          .expireAfterWrite(Duration.ofMinutes(10))
          .build(CacheLoader.from(
              location -> new LocationKey(location.getWorld().getName(), location.getBlockX(),
                  location.getBlockY(), location.getBlockZ())));

  public static LocationKey create(@NotNull Location loc) {
    return CACHE.getUnchecked(loc);
  }

  @Override
  public String toString() {
    return worldName + ":" + x + "," + y + "," + z;
  }
}
