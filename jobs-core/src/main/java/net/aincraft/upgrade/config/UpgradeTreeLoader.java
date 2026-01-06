package net.aincraft.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import net.aincraft.registry.Registry;
import net.aincraft.upgrade.UpgradeTree;
import org.bukkit.plugin.Plugin;

/**
 * Loads upgrade trees from JSON configuration files.
 */
public final class UpgradeTreeLoader {

  private static final String CONFIG_FILE = "upgrade_trees.json";
  private static final String DEFAULT_RESOURCE = "upgrade_trees_default.json";

  private final Plugin plugin;
  private final Gson gson;
  private final UpgradeTreeConfigParser parser;
  private final Registry<UpgradeTree> registry;

  public UpgradeTreeLoader(
      Plugin plugin,
      Gson gson,
      Registry<UpgradeTree> registry
  ) {
    this.plugin = plugin;
    this.gson = gson;
    this.parser = new UpgradeTreeConfigParser();
    this.registry = registry;
  }

  /**
   * Load upgrade trees from configuration file.
   * Creates default config if it doesn't exist.
   *
   * @return number of upgrade trees loaded
   */
  public int load() {
    File configFile = new File(plugin.getDataFolder(), CONFIG_FILE);

    if (!configFile.exists()) {
      createDefaultConfig(configFile);
    }

    try (FileReader reader = new FileReader(configFile)) {
      return loadFromReader(reader);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to load upgrade trees: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Reload upgrade trees from configuration file.
   *
   * @return number of upgrade trees loaded
   */
  public int reload() {
    return load();
  }

  private int loadFromReader(java.io.Reader reader) {
    JsonObject root = gson.fromJson(reader, JsonObject.class);
    if (root == null || !root.has("upgrade_trees")) {
      plugin.getLogger().warning("No 'upgrade_trees' key found in configuration");
      return 0;
    }

    JsonObject upgradeTrees = root.getAsJsonObject("upgrade_trees");
    int count = 0;

    for (Map.Entry<String, JsonElement> entry : upgradeTrees.entrySet()) {
      try {
        UpgradeTreeConfig config = gson.fromJson(entry.getValue(), UpgradeTreeConfig.class);
        UpgradeTree tree = parser.parse(config);
        registry.register(tree);
        count++;
        plugin.getLogger().info("Loaded upgrade tree for job: " + config.job());
      } catch (Exception e) {
        plugin.getLogger().warning(
            "Failed to parse upgrade tree '" + entry.getKey() + "': " + e.getMessage()
        );
      }
    }

    plugin.getLogger().info("Loaded " + count + " upgrade tree(s)");
    return count;
  }

  private void createDefaultConfig(File configFile) {
    try {
      if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
      }

      try (InputStream resource = plugin.getResource(DEFAULT_RESOURCE)) {
        if (resource != null) {
          Files.copy(resource, configFile.toPath());
          plugin.getLogger().info("Created default upgrade trees configuration");
          return;
        }
      }

      // Create minimal example config
      String defaultJson = """
          {
            "upgrade_trees": {
              "miner": {
                "job": "miner",
                "skill_points_per_level": 1,
                "root": "mining_basics",
                "nodes": {
                  "mining_basics": {
                    "name": "Mining Basics",
                    "description": "The foundation of all mining knowledge",
                    "icon": "WOODEN_PICKAXE",
                    "cost": 0,
                    "children": ["efficiency_1", "fortune_1"]
                  },
                  "efficiency_1": {
                    "name": "Efficiency I",
                    "description": "Mine 10% faster",
                    "icon": "IRON_PICKAXE",
                    "cost": 1,
                    "prerequisites": ["mining_basics"],
                    "effects": [
                      {"type": "boost", "target": "xp", "amount": 1.1}
                    ]
                  },
                  "fortune_1": {
                    "name": "Fortune I",
                    "description": "10% more drops",
                    "icon": "DIAMOND",
                    "cost": 1,
                    "prerequisites": ["mining_basics"],
                    "effects": [
                      {"type": "boost", "target": "money", "amount": 1.1}
                    ]
                  }
                }
              }
            }
          }
          """;
      Files.writeString(configFile.toPath(), defaultJson);
      plugin.getLogger().info("Created minimal upgrade trees configuration");
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to create default config: " + e.getMessage());
    }
  }
}
