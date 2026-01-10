package net.aincraft.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
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
  private static final String TREES_FOLDER = "upgrade_trees";

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
      BoostFactory boostFactory
  ) {
    this.plugin = plugin;
    this.gson = gson;
    this.legacyParser = new UpgradeTreeConfigParser(conditionFactory, boostFactory);
    this.wynncraftParser = new WynncraftTreeConfigParser(conditionFactory, boostFactory);
    this.wynncraftDeserializer = new WynncraftTreeConfigDeserializer();
    this.registry = registry;
  }

  /**
   * Load upgrade trees from configuration file.
   * Tries folder first, falls back to single file.
   *
   * @return number of upgrade trees loaded
   */
  public int load() {
    // Try folder-based loading first
    File treesFolder = new File(plugin.getDataFolder(), TREES_FOLDER);
    if (treesFolder.exists() && treesFolder.isDirectory()) {
      int count = loadFromFolder(treesFolder);
      if (count > 0) {
        plugin.getLogger().info("Loaded " + count + " tree(s) from folder: " + TREES_FOLDER);
        return count;
      }
    }

    // Fallback to single-file loading
    File configFile = new File(plugin.getDataFolder(), CONFIG_FILE);
    if (!configFile.exists()) {
      // Create folder structure for future saves
      if (!treesFolder.exists()) {
        treesFolder.mkdirs();
      }
      createDefaultConfig(configFile);
    }

    try (FileReader reader = new FileReader(configFile)) {
      int count = loadFromReader(reader);
      plugin.getLogger().info("Loaded " + count + " tree(s) from file: " + CONFIG_FILE);
      return count;
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to load upgrade trees: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Load upgrade trees from a folder containing individual JSON files.
   *
   * @param folder the folder containing tree JSON files
   * @return number of upgrade trees loaded
   */
  public int loadFromFolder(File folder) {
    int count = 0;
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

    if (files == null || files.length == 0) {
      plugin.getLogger().info("No JSON files found in folder: " + folder.getName());
      return 0;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        JsonObject treeObj = gson.fromJson(reader, JsonObject.class);
        if (treeObj == null) continue;

        // Get tree ID from filename (without .json extension)
        String treeId = file.getName().replace(".json", "");

        // Inject tree_id if not present
        if (!treeObj.has("tree_id")) {
          treeObj.addProperty("tree_id", treeId);
        }

        // Check if Wynncraft format (has layout)
        UpgradeTree tree;
        if (treeObj.has("layout")) {
          net.aincraft.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(treeObj, net.aincraft.upgrade.wynncraft.WynncraftTreeConfig.class, null);
          tree = wynncraftParser.parse(config);
        } else {
          // Legacy format - needs wrapping
          UpgradeTreeConfig config = gson.fromJson(treeObj, UpgradeTreeConfig.class);
          tree = legacyParser.parse(config);

          // Verify all nodes have positions (Wynncraft format is required now)
          boolean missingPositions = tree.allNodes().stream()
              .anyMatch(node -> node.position() == null);

          if (missingPositions) {
            throw new IllegalArgumentException(
                "Tree '" + treeId + "' uses legacy format but is missing positions. " +
                "Please use Wynncraft format with layout coordinates or specify positions for all nodes."
            );
          }
        }

        registry.register(tree);
        count++;
        plugin.getLogger().info("Loaded tree: " + treeId + " (job=" + tree.jobKey() + ")");

      } catch (Exception e) {
        plugin.getLogger().warning("Failed to load tree from " + file.getName() + ": " + e.getMessage());
        e.printStackTrace();
      }
    }

    return count;
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
          tree.allNodes().size() + " nodes");
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
              tree.allNodes().size() + " nodes");
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
              tree.allNodes().size() + " nodes");
          registry.register(tree);
          count++;
          continue;
        }

        // Parse as legacy format
        plugin.getLogger().info("Tree '" + entry.getKey() + "' uses legacy format");
        UpgradeTreeConfig config = gson.fromJson(entry.getValue(), UpgradeTreeConfig.class);
        tree = legacyParser.parse(config);

        // Verify all nodes have positions (Wynncraft format is required now)
        boolean missingPositions = tree.allNodes().stream()
            .anyMatch(node -> node.position() == null);

        if (missingPositions) {
          throw new IllegalArgumentException(
              "Tree '" + entry.getKey() + "' uses legacy format but is missing positions. " +
              "Please use Wynncraft format with layout coordinates or specify positions for all nodes."
          );
        }

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
   * Save an individual tree to a JSON file in the upgrade_trees folder.
   *
   * @param treeId the tree ID (used as filename without .json extension)
   * @param json the JSON content to write
   * @return true if saved successfully
   */
  public boolean saveTree(String treeId, String json) {
    File treesFolder = new File(plugin.getDataFolder(), TREES_FOLDER);
    if (!treesFolder.exists()) {
      treesFolder.mkdirs();
    }

    File treeFile = new File(treesFolder, treeId + ".json");
    try (FileWriter writer = new FileWriter(treeFile)) {
      gson.toJson(new com.google.gson.JsonParser().parse(json), writer);
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to save tree " + treeId + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }

    plugin.getLogger().info("Saved tree: " + treeId + " to " + treeFile.getPath());

    // Reload tree into registry so changes take effect immediately (after file is closed)
    loadSingleTree(treeId).ifPresent(tree -> {
      registry.register(tree);
      plugin.getLogger().info("Reloaded tree into registry: " + treeId);
    });

    return true;
  }

  /**
   * Get the folder where individual tree files are stored.
   *
   * @return the upgrade_trees folder
   */
  public File getTreesFolder() {
    File folder = new File(plugin.getDataFolder(), TREES_FOLDER);
    if (!folder.exists()) {
      folder.mkdirs();
    }
    return folder;
  }

  /**
   * Load a single tree from its JSON file.
   * Useful for reloading after manual edits.
   *
   * @param treeId the tree ID (filename without .json extension)
   * @return the loaded tree, or empty if not found
   */
  public java.util.Optional<UpgradeTree> loadSingleTree(String treeId) {
    File treeFile = new File(getTreesFolder(), treeId + ".json");
    if (!treeFile.exists()) {
      return java.util.Optional.empty();
    }

    try (FileReader reader = new FileReader(treeFile)) {
      JsonObject treeObj = gson.fromJson(reader, JsonObject.class);
      if (treeObj == null) {
        return java.util.Optional.empty();
      }

      // Inject tree_id if not present
      if (!treeObj.has("tree_id")) {
        treeObj.addProperty("tree_id", treeId);
      }

      // Check if Wynncraft format (has layout)
      UpgradeTree tree;
      if (treeObj.has("layout")) {
        net.aincraft.upgrade.wynncraft.WynncraftTreeConfig config =
            wynncraftDeserializer.deserialize(treeObj, net.aincraft.upgrade.wynncraft.WynncraftTreeConfig.class, null);
        tree = wynncraftParser.parse(config);
      } else {
        // Legacy format - requires manual positions
        UpgradeTreeConfig config = gson.fromJson(treeObj, UpgradeTreeConfig.class);
        tree = legacyParser.parse(config);

        // Verify all nodes have positions
        boolean missingPositions = tree.allNodes().stream()
            .anyMatch(node -> node.position() == null);

        if (missingPositions) {
          throw new IllegalArgumentException(
              "Tree '" + treeId + "' uses legacy format but is missing positions. " +
              "Please use Wynncraft format with layout coordinates or specify positions for all nodes."
          );
        }
      }

      return java.util.Optional.of(tree);
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to load tree '" + treeId + "': " + e.getMessage());
      e.printStackTrace();
      return java.util.Optional.empty();
    }
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
