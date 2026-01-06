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
import net.aincraft.gui.SugiyamaLayout;
import net.aincraft.registry.Registry;
import net.aincraft.upgrade.UpgradeNode.Position;
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

        // Check if tree already has manual positions
        boolean hasManualPositions = tree.allNodes().stream()
            .anyMatch(node -> node.position() != null);

        Map<String, Position> positions;
        if (hasManualPositions) {
          // Use manual positions from JSON, only auto-generate missing ones
          positions = new java.util.HashMap<>();
          plugin.getLogger().info("Using manual positions for " + config.job() + ":");

          // First, collect all manual positions
          for (net.aincraft.upgrade.UpgradeNode node : tree.allNodes()) {
            if (node.position() != null) {
              String key = getShortKey(node);
              positions.put(key, node.position());
              plugin.getLogger().info("  " + key + " -> manual (" + node.position().x() + ", " + node.position().y() + ")");
            }
          }

          // Auto-generate only for nodes without positions
          Map<String, Position> autoPositions = SugiyamaLayout.generateLayout(tree);
          for (net.aincraft.upgrade.UpgradeNode node : tree.allNodes()) {
            String key = getShortKey(node);
            if (node.position() == null && autoPositions.containsKey(key)) {
              Position pos = autoPositions.get(key);
              positions.put(key, pos);
              plugin.getLogger().info("  " + key + " -> auto (" + pos.x() + ", " + pos.y() + ")");
            }
          }
        } else {
          // No manual positions, auto-generate everything
          positions = SugiyamaLayout.generateLayout(tree);
          plugin.getLogger().info("Generated auto layout for " + config.job() + ":");
          for (Map.Entry<String, Position> posEntry : positions.entrySet()) {
            Position pos = posEntry.getValue();
            plugin.getLogger().info("  " + posEntry.getKey() + " -> (" + pos.x() + ", " + pos.y() + ")");
          }
        }

        // Apply positions to tree
        tree = applyPositions(tree, positions);

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

  /**
   * Apply generated positions to an existing upgrade tree by creating new node instances.
   */
  private UpgradeTree applyPositions(UpgradeTree tree, Map<String, Position> positions) {
    Map<String, net.aincraft.upgrade.UpgradeNode> updatedNodes = new java.util.HashMap<>();

    for (net.aincraft.upgrade.UpgradeNode node : tree.allNodes()) {
      String shortKey = getShortKey(node);
      Position newPosition = positions.get(shortKey);

      // Create new node with updated position (or keep existing if no new position)
      net.aincraft.upgrade.UpgradeNode updatedNode = new net.aincraft.upgrade.UpgradeNode(
          node.key(),
          node.name(),
          node.description(),
          node.icon(),
          node.cost(),
          node.nodeType(),
          node.prerequisites(),
          node.exclusive(),
          node.children(),
          node.effects(),
          newPosition != null ? newPosition : node.position()
      );
      updatedNodes.put(shortKey, updatedNode);
    }

    // Create new tree with updated nodes
    return new UpgradeTree(
        tree.key(),
        tree.jobKey(),
        tree.rootNodeKey(),
        tree.skillPointsPerLevel(),
        updatedNodes
    );
  }

  private String getShortKey(net.aincraft.upgrade.UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
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
