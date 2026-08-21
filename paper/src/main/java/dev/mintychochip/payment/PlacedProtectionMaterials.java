package dev.mintychochip.payment;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loads place→break anti-farm material durations from {@code exploit-config.yml} under
 * {@code placed}. Defaults protect a broad farm set (not STONE-only); {@code *} expands to every
 * resolvable block material plus the curated farm set.
 */
public final class PlacedProtectionMaterials {

  public static final long DEFAULT_DURATION_SECONDS = 60L;

  /**
   * Curated materials commonly used in place→break farms. Always included for {@code *} / defaults
   * so production is never STONE-only even if full Material scans are partial.
   */
  private static final Material[] CURATED_FARM_MATERIALS = {
      Material.STONE,
      Material.COBBLESTONE,
      Material.DIRT,
      Material.GRASS_BLOCK,
      Material.COARSE_DIRT,
      Material.ROOTED_DIRT,
      Material.MUD,
      Material.SAND,
      Material.RED_SAND,
      Material.GRAVEL,
      Material.CLAY,
      Material.NETHERRACK,
      Material.END_STONE,
      Material.SOUL_SAND,
      Material.SOUL_SOIL,
      Material.BASALT,
      Material.BLACKSTONE,
      Material.DEEPSLATE,
      Material.TUFF,
      Material.CALCITE,
      Material.ANDESITE,
      Material.DIORITE,
      Material.GRANITE,
      Material.OAK_LOG,
      Material.SPRUCE_LOG,
      Material.BIRCH_LOG,
      Material.JUNGLE_LOG,
      Material.ACACIA_LOG,
      Material.DARK_OAK_LOG,
      Material.MANGROVE_LOG,
      Material.CHERRY_LOG,
      Material.PALE_OAK_LOG,
      Material.CRIMSON_STEM,
      Material.WARPED_STEM,
      Material.COAL_ORE,
      Material.IRON_ORE,
      Material.COPPER_ORE,
      Material.GOLD_ORE,
      Material.REDSTONE_ORE,
      Material.LAPIS_ORE,
      Material.DIAMOND_ORE,
      Material.EMERALD_ORE,
      Material.DEEPSLATE_COAL_ORE,
      Material.DEEPSLATE_IRON_ORE,
      Material.DEEPSLATE_COPPER_ORE,
      Material.DEEPSLATE_GOLD_ORE,
      Material.DEEPSLATE_REDSTONE_ORE,
      Material.DEEPSLATE_LAPIS_ORE,
      Material.DEEPSLATE_DIAMOND_ORE,
      Material.DEEPSLATE_EMERALD_ORE,
      Material.NETHER_GOLD_ORE,
      Material.NETHER_QUARTZ_ORE,
      Material.ANCIENT_DEBRIS,
      Material.WHEAT,
      Material.CARROTS,
      Material.POTATOES,
      Material.BEETROOTS,
      Material.NETHER_WART,
      Material.SUGAR_CANE,
      Material.BAMBOO,
      Material.CACTUS,
      Material.KELP,
      Material.MELON,
      Material.PUMPKIN,
      Material.COCOA,
      Material.SWEET_BERRY_BUSH,
  };

  private PlacedProtectionMaterials() {}

  /** Curated farm materials (for tests and fallbacks). */
  public static Set<Material> curatedFarmMaterials() {
    return EnumSet.copyOf(List.of(CURATED_FARM_MATERIALS));
  }

  /**
   * Default map: curated farm materials plus every block Material that resolves under the current
   * server/runtime ({@code *} semantics).
   */
  public static Map<Material, Duration> allBlocks(Duration duration) {
    Map<Material, Duration> map = new EnumMap<>(Material.class);
    for (Material material : Material.values()) {
      if (isProtectableBlock(material)) {
        map.put(material, duration);
      }
    }
    for (Material material : CURATED_FARM_MATERIALS) {
      map.put(material, duration);
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Default map: all protectable blocks with the default duration ({@code *} semantics).
   */
  public static Map<Material, Duration> defaults() {
    return allBlocks(Duration.ofSeconds(DEFAULT_DURATION_SECONDS));
  }

  /**
   * Load from plugin's {@code exploit-config.yml} if present; otherwise defaults.
   */
  public static Map<Material, Duration> load(@NotNull Plugin plugin) {
    plugin.saveResource("exploit-config.yml", false);
    java.io.File file = new java.io.File(plugin.getDataFolder(), "exploit-config.yml");
    FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    return fromConfiguration(yaml, plugin.getLogger());
  }

  /**
   * Parse a configuration root that may contain a {@code placed} section.
   * Pure enough for unit tests (pass any {@link FileConfiguration}).
   */
  public static Map<Material, Duration> fromConfiguration(
      @NotNull FileConfiguration config, @NotNull Logger logger) {
    ConfigurationSection placed = config.getConfigurationSection("placed");
    long seconds = DEFAULT_DURATION_SECONDS;
    List<String> materials = List.of("*");

    if (placed != null) {
      seconds = placed.getLong("duration-seconds", DEFAULT_DURATION_SECONDS);
      if (seconds <= 0) {
        logger.warning(
            "placed.duration-seconds must be > 0 (got " + seconds + "); using "
                + DEFAULT_DURATION_SECONDS);
        seconds = DEFAULT_DURATION_SECONDS;
      }
      List<String> listed = placed.getStringList("materials");
      if (!listed.isEmpty()) {
        materials = listed;
      }
    }

    Duration duration = Duration.ofSeconds(seconds);
    for (String entry : materials) {
      if (entry == null) {
        continue;
      }
      String token = entry.trim();
      if (token.equals("*") || token.equalsIgnoreCase("ALL")) {
        return allBlocks(duration);
      }
    }

    Map<Material, Duration> map = new EnumMap<>(Material.class);
    for (String entry : materials) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      String name = entry.trim().toUpperCase(Locale.ROOT);
      try {
        Material material = Material.valueOf(name);
        if (isProtectableBlock(material) || curatedFarmMaterials().contains(material)) {
          map.put(material, duration);
        } else {
          logger.warning("placed.materials: " + name + " is not a placeable block; skipping");
        }
      } catch (IllegalArgumentException e) {
        logger.warning("placed.materials: unknown material " + name + "; skipping");
      }
    }

    if (map.isEmpty()) {
      logger.warning("placed.materials resolved empty; falling back to defaults");
      return allBlocks(duration);
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Safe block check: never throws on incomplete Material metadata (e.g. MockBukkit).
   */
  static boolean isProtectableBlock(Material material) {
    if (material == null) {
      return false;
    }
    try {
      if (material.isAir()) {
        return false;
      }
      return material.isBlock();
    } catch (UnsupportedOperationException | LinkageError e) {
      // Incomplete registry entry — treat known non-air names as candidates only via curated set
      String name = material.name();
      return !name.equals("AIR") && !name.equals("CAVE_AIR") && !name.equals("VOID_AIR");
    }
  }
}
