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
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.gui.SugiyamaLayout;
import net.aincraft.registry.Registry;
import net.aincraft.upgrade.Position;
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
  private final UpgradeTreeConfigParser legacyParser;
  private final WynncraftTreeConfigParser wynncraftParser;
  private final WynncraftTreeConfigDeserializer wynncraftDeserializer;
  private final Registry<UpgradeTree> registry;

  public UpgradeTreeLoader(
      Plugin plugin,
      Gson gson,
      Registry<UpgradeTree> registry,
      ConditionFactory conditionFactory,
      PolicyFactory policyFactory,
      BoostFactory boostFactory
  ) {
    this.plugin = plugin;
    this.gson = gson;
    this.legacyParser = new UpgradeTreeConfigParser(conditionFactory, policyFactory, boostFactory);
    this.wynncraftParser = new WynncraftTreeConfigParser(conditionFactory, policyFactory, boostFactory);
    this.wynncraftDeserializer = new WynncraftTreeConfigDeserializer();
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
    if (root == null) {
      plugin.getLogger().warning("Failed to parse JSON configuration");
      return 0;
    }

    // Detect format: Wynncraft format has "layout" array at root level
    boolean isWynncraftFormat = root.has("layout");

    if (isWynncraftFormat) {
      return loadWynncraftFormat(root);
    } else {
      return loadLegacyFormat(root);
    }
  }

  /**
   * Load Wynncraft-style format (with "layout" array).
   */
  private int loadWynncraftFormat(JsonObject root) {
    try {
      plugin.getLogger().info("Detected Wynncraft format - using WynncraftTreeConfigParser");

      // Wynncraft format can be either:
      // 1. Flat format: { "tree_id": "...", "job": "...", "layout": [...] }
      // 2. Nested format: { "miner_v1": { "metadata": {...}, "layout": [...] } }

      // Check if it's nested format by looking for a tree entry (has "layout" as a child)
      boolean isNestedFormat = false;
      for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
        JsonElement value = entry.getValue();
        if (value.isJsonObject()) {
          JsonObject obj = value.getAsJsonObject();
          if (obj.has("layout") && obj.has("metadata")) {
            isNestedFormat = true;
            break;
          }
        }
      }

      if (isNestedFormat) {
        return loadNestedWynncraftFormat(root);
      } else {
        return loadFlatWynncraftFormat(root);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to parse Wynncraft format: " + e.getMessage());
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Load flat Wynncraft format where tree_id, job, layout are at root level.
   */
  private int loadFlatWynncraftFormat(JsonObject root) {
    try {
      plugin.getLogger().info("Loading flat Wynncraft format");

      // Deserialize directly - root is the tree config
      net.aincraft.upgrade.wynncraft.WynncraftTreeConfig config =
          wynncraftDeserializer.deserialize(root, net.aincraft.upgrade.wynncraft.WynncraftTreeConfig.class, null);

      // Parse into UpgradeTree
      UpgradeTree tree = wynncraftParser.parse(config);

      // Wynncraft format always includes positions from coordinates
      registry.register(tree);

      plugin.getLogger().info("Loaded Wynncraft upgrade tree: " + config.treeId() + " (jobKey=" + tree.jobKey() + ") with " +
          tree.allNodes().size() + " nodes and " + tree.allConnectors().size() + " connectors");
      return 1;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to parse flat Wynncraft format: " + e.getMessage());
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Load nested Wynncraft format where tree entries are keyed by tree ID.
   */
  private int loadNestedWynncraftFormat(JsonObject root) {
    try {
      plugin.getLogger().info("Loading nested Wynncraft format");

      int count = 0;

      // Wynncraft format has tree entries as keys (e.g., "miner_v1": { ... })
      // We need to iterate through each entry and parse it
      for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
        try {
          String treeId = entry.getKey();
          JsonObject treeConfig = entry.getValue().getAsJsonObject();

          // Skip entries that don't look like tree configs
          if (!treeConfig.has("layout") || !treeConfig.has("metadata")) {
            continue;
          }

          // Inject tree_id into the config for the deserializer
          treeConfig.addProperty("tree_id", treeId);

          // Deserialize using custom deserializer
          net.aincraft.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(treeConfig, net.aincraft.upgrade.wynncraft.WynncraftTreeConfig.class, null);

          // Parse into UpgradeTree
          UpgradeTree tree = wynncraftParser.parse(config);

          // Wynncraft format always includes positions from coordinates
          // No auto-generation needed
          registry.register(tree);

          plugin.getLogger().info("Loaded Wynncraft upgrade tree: " + treeId + " (jobKey=" + tree.jobKey() + ") with " +
              tree.allNodes().size() + " nodes and " + tree.allConnectors().size() + " connectors");
          count++;
        } catch (Exception e) {
          plugin.getLogger().warning("Failed to parse Wynncraft tree '" + entry.getKey() + "': " + e.getMessage());
          e.printStackTrace();
        }
      }

      return count;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to parse nested Wynncraft format: " + e.getMessage());
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Load legacy format (with "upgrade_trees" object).
   * Also handles hybrid format where trees inside use Wynncraft-style layout.
   */
  private int loadLegacyFormat(JsonObject root) {
    if (!root.has("upgrade_trees")) {
      plugin.getLogger().warning("No 'upgrade_trees' key found in configuration");
      return 0;
    }

    plugin.getLogger().info("Detected wrapped format - checking individual trees");

    JsonObject upgradeTrees = root.getAsJsonObject("upgrade_trees");
    int count = 0;

    for (Map.Entry<String, JsonElement> entry : upgradeTrees.entrySet()) {
      try {
        JsonObject treeConfig = entry.getValue().getAsJsonObject();

        // Check if this tree uses Wynncraft format (has layout) or legacy format (has nodes)
        boolean isWynncraftTree = treeConfig.has("layout");

        UpgradeTree tree;
        if (isWynncraftTree) {
          // Parse as Wynncraft format
          plugin.getLogger().info("Tree '" + entry.getKey() + "' uses Wynncraft format");

          // Inject tree_id if not present
          if (!treeConfig.has("tree_id")) {
            treeConfig.addProperty("tree_id", entry.getKey());
          }

          net.aincraft.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(treeConfig, net.aincraft.upgrade.wynncraft.WynncraftTreeConfig.class, null);
          tree = wynncraftParser.parse(config);

          plugin.getLogger().info("Loaded Wynncraft upgrade tree: " + entry.getKey() + " (jobKey=" + tree.jobKey() + ") with " +
              tree.allNodes().size() + " nodes and " + tree.allConnectors().size() + " connectors");
          registry.register(tree);
          count++;
          continue;
        }

        // Parse as legacy format
        plugin.getLogger().info("Tree '" + entry.getKey() + "' uses legacy format");
        UpgradeTreeConfig config = gson.fromJson(entry.getValue(), UpgradeTreeConfig.class);
        tree = legacyParser.parse(config);

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
        plugin.getLogger().info("Loaded legacy upgrade tree for job: " + config.job() + " (jobKey=" + tree.jobKey() + ") with " +
            tree.allNodes().size() + " nodes");
      } catch (Exception e) {
        plugin.getLogger().warning(
            "Failed to parse upgrade tree '" + entry.getKey() + "': " + e.getMessage()
        );
        e.printStackTrace();
      }
    }

    plugin.getLogger().info("Loaded " + count + " upgrade tree(s) from legacy format");
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
          newPosition != null ? newPosition : node.position(),
          node.pathPoints(), // preserve path points
          node.perkId(),
          node.level()
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
