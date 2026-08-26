package dev.mintychochip.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable block-location key using world name and integer block coordinates.
 *
 * @param worldName world name
 * @param x block X coordinate
 * @param y block Y coordinate
 * @param z block Z coordinate
 */
public record LocationKey(@NotNull String worldName, int x, int y, int z) {

  /**
   * Short-lived cache of location-to-block-key conversions.
   *
   * <p>The cache expires entries after ten minutes; it is an optimization only.
   */
  public static final LoadingCache<Location, LocationKey> CACHE =
      CacheBuilder.newBuilder()
          .expireAfterWrite(Duration.ofMinutes(10))
          .build(
              CacheLoader.from(
                  location ->
                      new LocationKey(
                          location.getWorld().getName(),
                          location.getBlockX(),
                          location.getBlockY(),
                          location.getBlockZ())));

  /**
   * Converts a Bukkit location to its block-coordinate key.
   *
   * @param loc source location; must not be {@code null}
   * @return cached immutable key
   * @throws com.google.common.cache.CacheLoader.InvalidCacheLoadException if the location's world
   *     is null
   */
  public static LocationKey create(@NotNull Location loc) {
    return CACHE.getUnchecked(loc);
  }

  /**
   * Formats this key as {@code world:x,y,z}.
   *
   * @return stable textual key
   */
  @Override
  public String toString() {
    return worldName + ":" + x + "," + y + "," + z;
  }
}
