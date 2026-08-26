package dev.mintychochip.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.upgrade.NodeEffect;
import dev.mintychochip.upgrade.NodeLevel;
import dev.mintychochip.upgrade.PerkPolicy;
import dev.mintychochip.upgrade.Position;
import dev.mintychochip.upgrade.Requirement;
import dev.mintychochip.upgrade.Requirements.NodeLevelRequirement;
import dev.mintychochip.upgrade.SkillNode;
import dev.mintychochip.upgrade.SkillNode.LevelEffectMode;
import dev.mintychochip.upgrade.SkillNodeKind;
import dev.mintychochip.upgrade.SkillTree;
import dev.mintychochip.upgrade.UpgradeEffect;
import dev.mintychochip.upgrade.UpgradeNode;
import dev.mintychochip.upgrade.UpgradeTree;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;

/** Loads upgrade trees from JSON configuration files. */
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
  private final Registry<SkillTree> skillTreeRegistry;
  private final SkillTreeConfigParser skillTreeParser;

  /** Upgrade tree loader. */
  public UpgradeTreeLoader(
      Plugin plugin,
      Gson gson,
      Registry<UpgradeTree> registry,
      Registry<SkillTree> skillTreeRegistry,
      ConditionFactory conditionFactory,
      BoostFactory boostFactory) {
    this.plugin = plugin;
    this.gson = gson;
    this.legacyParser = new UpgradeTreeConfigParser(conditionFactory, boostFactory);
    this.wynncraftParser = new WynncraftTreeConfigParser(conditionFactory, boostFactory);
    this.wynncraftDeserializer = new WynncraftTreeConfigDeserializer();
    this.registry = registry;
    this.skillTreeRegistry = skillTreeRegistry;
    this.skillTreeParser = new SkillTreeConfigParser(boostFactory, conditionFactory);
  }

  /**
   * Load upgrade trees from configuration file. Tries folder first, falls back to single file.
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

    try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
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
      try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
        JsonObject treeObj = gson.fromJson(reader, JsonObject.class);
        if (treeObj == null) {
          continue;
        }

        // Get tree ID from filename (without .json extension)
        String treeId = file.getName().replace(".json", "");

        // Inject tree_id if not present
        if (!treeObj.has("tree_id")) {
          treeObj.addProperty("tree_id", treeId);
        }

        // Version-2 skill tree files bypass the legacy/Wynncraft parsers.
        if (isV2(treeObj)) {
          SkillTree tree = skillTreeParser.parse(treeObj);
          skillTreeRegistry.register(tree);
          plugin
              .getLogger()
              .info("Loaded v2 skill tree: " + treeId + " (jobKey=" + tree.jobKey() + ")");
          count++;
          continue;
        }

        // Check if Wynncraft format (has layout)
        UpgradeTree tree;
        if (treeObj.has("layout")) {
          dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(
                  treeObj, dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig.class, null);
          tree = wynncraftParser.parse(config);
        } else {
          // Legacy format - needs wrapping
          UpgradeTreeConfig config = gson.fromJson(treeObj, UpgradeTreeConfig.class);
          tree = legacyParser.parse(config);

          // Verify all nodes have positions (Wynncraft format is required now)
          boolean missingPositions =
              tree.allNodes().stream().anyMatch(node -> node.position() == null);

          if (missingPositions) {
            throw new IllegalArgumentException(
                "Tree '"
                    + treeId
                    + "' uses legacy format but is missing positions. "
                    + "Please use Wynncraft format with layout coordinates or specify positions"
                    + " for all nodes.");
          }
        }

        registry.register(tree);
        convertLegacy(tree);
        count++;
        plugin.getLogger().info("Loaded tree: " + treeId + " (job=" + tree.jobKey() + ")");

      } catch (IOException
          | JsonParseException
          | IllegalArgumentException
          | IllegalStateException e) {
        plugin
            .getLogger()
            .warning("Failed to load tree from " + file.getName() + ": " + e.getMessage());
        plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
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

  private int loadFromReader(Reader reader) {
    JsonObject root = gson.fromJson(reader, JsonObject.class);
    if (root == null) {
      plugin.getLogger().warning("Failed to parse JSON configuration");
      return 0;
    }

    // A single-file config may itself be a version-2 skill tree.
    if (isV2(root)) {
      try {
        SkillTree tree = skillTreeParser.parse(root);
        skillTreeRegistry.register(tree);
        plugin
            .getLogger()
            .info("Loaded v2 skill tree: " + CONFIG_FILE + " (jobKey=" + tree.jobKey() + ")");
        return 1;
      } catch (IllegalArgumentException | IllegalStateException e) {
        plugin.getLogger().warning("Failed to parse v2 skill tree: " + e.getMessage());
        return 0;
      }
    }

    // Detect format: Wynncraft format has "layout" array at root level
    boolean isWynncraftFormat = root.has("layout");

    if (isWynncraftFormat) {
      return loadWynncraftFormat(root);
    }

    // Root holds v2 trees keyed by tree id (no "upgrade_trees" wrapper).
    if (hasV2Children(root)) {
      return loadNestedV2Format(root);
    }

    return loadLegacyFormat(root);
  }

  /**
   * Load a config whose entries are version-2 skill trees keyed by tree id. Only v2 entries are
   * consumed here; mixed legacy entries belong in the {@code upgrade_trees} wrapper handled by
   * {@link #loadLegacyFormat}.
   */
  private int loadNestedV2Format(JsonObject root) {
    int count = 0;
    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
      if (!entry.getValue().isJsonObject() || !isV2(entry.getValue().getAsJsonObject())) {
        continue;
      }
      try {
        SkillTree tree = skillTreeParser.parse(entry.getValue().getAsJsonObject());
        skillTreeRegistry.register(tree);
        plugin
            .getLogger()
            .info("Loaded v2 skill tree: " + entry.getKey() + " (jobKey=" + tree.jobKey() + ")");
        count++;
      } catch (IllegalArgumentException | IllegalStateException e) {
        plugin
            .getLogger()
            .warning("Failed to parse v2 tree '" + entry.getKey() + "': " + e.getMessage());
        plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      }
    }
    return count;
  }

  private static boolean hasV2Children(JsonObject root) {
    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
      if (entry.getValue().isJsonObject() && isV2(entry.getValue().getAsJsonObject())) {
        return true;
      }
    }
    return false;
  }

  /** Load Wynncraft-style format (with "layout" array). */
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
    } catch (IllegalStateException e) {
      plugin.getLogger().warning("Failed to parse Wynncraft format: " + e.getMessage());
      plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      return 0;
    }
  }

  /** Load flat Wynncraft format where tree_id, job, layout are at root level. */
  private int loadFlatWynncraftFormat(JsonObject root) {
    try {
      plugin.getLogger().info("Loading flat Wynncraft format");

      // A version-2 tree in flat form is parsed by the v2 parser.
      if (isV2(root)) {
        SkillTree tree = skillTreeParser.parse(root);
        skillTreeRegistry.register(tree);
        plugin.getLogger().info("Loaded v2 skill tree (jobKey=" + tree.jobKey() + ")");
        return 1;
      }

      // Deserialize directly - root is the tree config
      dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig config =
          wynncraftDeserializer.deserialize(
              root, dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig.class, null);

      // Parse into UpgradeTree
      UpgradeTree tree = wynncraftParser.parse(config);

      // Wynncraft format always includes positions from coordinates
      registry.register(tree);
      convertLegacy(tree);

      plugin
          .getLogger()
          .info(
              "Loaded Wynncraft upgrade tree: "
                  + config.treeId()
                  + " (jobKey="
                  + tree.jobKey()
                  + ") with "
                  + tree.allNodes().size()
                  + " nodes");
      return 1;
    } catch (JsonParseException | IllegalArgumentException | IllegalStateException e) {
      plugin.getLogger().warning("Failed to parse flat Wynncraft format: " + e.getMessage());
      plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      return 0;
    }
  }

  /** Load nested Wynncraft format where tree entries are keyed by tree ID. */
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

          // Version-2 trees skip the metadata/layout requirements.
          if (isV2(treeConfig)) {
            SkillTree tree = skillTreeParser.parse(treeConfig);
            skillTreeRegistry.register(tree);
            plugin
                .getLogger()
                .info("Loaded v2 skill tree: " + treeId + " (jobKey=" + tree.jobKey() + ")");
            count++;
            continue;
          }

          // Skip entries that don't look like tree configs
          if (!treeConfig.has("layout") || !treeConfig.has("metadata")) {
            continue;
          }

          // Inject tree_id into the config for the deserializer
          treeConfig.addProperty("tree_id", treeId);

          // Deserialize using custom deserializer
          dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(
                  treeConfig, dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig.class, null);

          // Parse into UpgradeTree
          UpgradeTree tree = wynncraftParser.parse(config);

          // Wynncraft format always includes positions from coordinates
          // No auto-generation needed
          registry.register(tree);
          convertLegacy(tree);

          plugin
              .getLogger()
              .info(
                  "Loaded Wynncraft upgrade tree: "
                      + treeId
                      + " (jobKey="
                      + tree.jobKey()
                      + ") with "
                      + tree.allNodes().size()
                      + " nodes");
          count++;
        } catch (JsonParseException | IllegalArgumentException | IllegalStateException e) {
          plugin
              .getLogger()
              .warning(
                  "Failed to parse Wynncraft tree '" + entry.getKey() + "': " + e.getMessage());
          plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
        }
      }

      return count;
    } catch (IllegalStateException e) {
      plugin.getLogger().warning("Failed to parse nested Wynncraft format: " + e.getMessage());
      plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      return 0;
    }
  }

  /**
   * Load legacy format (with "upgrade_trees" object). Also handles hybrid format where trees inside
   * use Wynncraft-style layout.
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

        // Version-2 entries bypass the legacy/wrapped Wynncraft parsers.
        if (isV2(treeConfig)) {
          SkillTree tree = skillTreeParser.parse(treeConfig);
          skillTreeRegistry.register(tree);
          plugin
              .getLogger()
              .info("Loaded v2 skill tree: " + entry.getKey() + " (jobKey=" + tree.jobKey() + ")");
          count++;
          continue;
        }

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

          dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig config =
              wynncraftDeserializer.deserialize(
                  treeConfig, dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig.class, null);
          tree = wynncraftParser.parse(config);

          plugin
              .getLogger()
              .info(
                  "Loaded Wynncraft upgrade tree: "
                      + entry.getKey()
                      + " (jobKey="
                      + tree.jobKey()
                      + ") with "
                      + tree.allNodes().size()
                      + " nodes");
          registry.register(tree);
          convertLegacy(tree);
          count++;
          continue;
        }

        // Parse as legacy format
        plugin.getLogger().info("Tree '" + entry.getKey() + "' uses legacy format");
        UpgradeTreeConfig config = gson.fromJson(entry.getValue(), UpgradeTreeConfig.class);
        tree = legacyParser.parse(config);

        // Verify all nodes have positions (Wynncraft format is required now)
        boolean missingPositions =
            tree.allNodes().stream().anyMatch(node -> node.position() == null);

        if (missingPositions) {
          throw new IllegalArgumentException(
              "Tree '"
                  + entry.getKey()
                  + "' uses legacy format but is missing positions. "
                  + "Please use Wynncraft format with layout coordinates or specify positions"
                  + " for all nodes.");
        }

        registry.register(tree);
        convertLegacy(tree);
        count++;
        plugin
            .getLogger()
            .info(
                "Loaded legacy upgrade tree for job: "
                    + config.job()
                    + " (jobKey="
                    + tree.jobKey()
                    + ") with "
                    + tree.allNodes().size()
                    + " nodes");
      } catch (JsonParseException | IllegalArgumentException | IllegalStateException e) {
        plugin
            .getLogger()
            .warning("Failed to parse upgrade tree '" + entry.getKey() + "': " + e.getMessage());
        plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
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
    try (Writer writer = Files.newBufferedWriter(treeFile.toPath(), StandardCharsets.UTF_8)) {
      gson.toJson(new com.google.gson.JsonParser().parse(json), writer);
    } catch (IOException | JsonParseException | IllegalStateException e) {
      plugin.getLogger().warning("Failed to save tree " + treeId + ": " + e.getMessage());
      plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      return false;
    }

    plugin.getLogger().info("Saved tree: " + treeId + " to " + treeFile.getPath());

    // Reload tree into registry so changes take effect immediately (after file is closed)
    loadSingleTree(treeId)
        .ifPresent(
            tree -> {
              registry.register(tree);
              convertLegacy(tree);
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
   * Load a single tree from its JSON file. Useful for reloading after manual edits.
   *
   * @param treeId the tree ID (filename without .json extension)
   * @return the loaded tree, or empty if not found
   */
  public java.util.Optional<UpgradeTree> loadSingleTree(String treeId) {
    File treeFile = new File(getTreesFolder(), treeId + ".json");
    if (!treeFile.exists()) {
      return java.util.Optional.empty();
    }

    try (Reader reader = Files.newBufferedReader(treeFile.toPath(), StandardCharsets.UTF_8)) {
      JsonObject treeObj = gson.fromJson(reader, JsonObject.class);
      if (treeObj == null) {
        return java.util.Optional.empty();
      }

      // Inject tree_id if not present
      if (!treeObj.has("tree_id")) {
        treeObj.addProperty("tree_id", treeId);
      }

      // Version-2 trees load into the v2 registry; they are not UpgradeTrees.
      if (isV2(treeObj)) {
        SkillTree tree = skillTreeParser.parse(treeObj);
        skillTreeRegistry.register(tree);
        plugin
            .getLogger()
            .info("Loaded v2 skill tree: " + treeId + " (jobKey=" + tree.jobKey() + ")");
        return java.util.Optional.empty();
      }

      // Check if Wynncraft format (has layout)
      UpgradeTree tree;
      if (treeObj.has("layout")) {
        dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig config =
            wynncraftDeserializer.deserialize(
                treeObj, dev.mintychochip.upgrade.wynncraft.WynncraftTreeConfig.class, null);
        tree = wynncraftParser.parse(config);
      } else {
        // Legacy format - requires manual positions
        UpgradeTreeConfig config = gson.fromJson(treeObj, UpgradeTreeConfig.class);
        tree = legacyParser.parse(config);

        // Verify all nodes have positions
        boolean missingPositions =
            tree.allNodes().stream().anyMatch(node -> node.position() == null);

        if (missingPositions) {
          throw new IllegalArgumentException(
              "Tree '"
                  + treeId
                  + "' uses legacy format but is missing positions. "
                  + "Please use Wynncraft format with layout coordinates or specify positions"
                  + " for all nodes.");
        }
      }

      return java.util.Optional.of(tree);
    } catch (IOException
        | JsonParseException
        | IllegalArgumentException
        | IllegalStateException e) {
      plugin.getLogger().warning("Failed to load tree '" + treeId + "': " + e.getMessage());
      plugin.getLogger().log(Level.WARNING, "Upgrade tree load failed", e);
      return java.util.Optional.empty();
    }
  }

  private static boolean isV2(JsonObject treeObj) {
    if (!treeObj.has("version") || !treeObj.get("version").isJsonPrimitive()) {
      return false;
    }
    JsonElement version = treeObj.get("version");
    if (!version.getAsJsonPrimitive().isNumber()) {
      return false;
    }
    try {
      return version.getAsInt() == 2;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Builds a version-2 {@link SkillTree} adapter for a legacy tree and registers it in the v2
   * registry. The original {@link UpgradeTree} stays registered in the legacy registry; load paths
   * register both so v2 service consumers see every tree while legacy callers keep their existing
   * view.
   *
   * <p>Legacy nodes are grouped by {@linkplain UpgradeNode#perkId() perk}, sorted by level, and
   * collapsed into one v2 {@link SkillNode} whose per-level costs/effects preserve the legacy
   * {@link PerkPolicy}: MAX maps to {@link LevelEffectMode#REPLACE}, ADDITIVE to {@link
   * LevelEffectMode#CUMULATIVE}. Numeric legacy references (e.g. {@code efficiency_1}) are remapped
   * to their perk node; exclusives become v2 excludes; maxed prerequisites become {@link
   * NodeLevelRequirement}s against the target perk's converted max level; positions and path points
   * carry over. Tree-level walk paths ({@link UpgradeTree#paths()}) and state writes have no
   * per-node v2 equivalent, so they are not migrated.
   */
  public SkillTree convertLegacy(UpgradeTree legacy) {
    Map<String, String> perkByNodeKey = new LinkedHashMap<>();
    for (UpgradeNode node : legacy.allNodes()) {
      String nodeKey = node.key().value();
      String perkId = node.perkId();
      perkByNodeKey.put(nodeKey, perkId == null || perkId.isBlank() ? nodeKey : perkId);
    }

    Map<String, List<UpgradeNode>> nodesByPerk = new LinkedHashMap<>();
    for (UpgradeNode node : legacy.allNodes()) {
      nodesByPerk
          .computeIfAbsent(perkByNodeKey.get(node.key().value()), k -> new ArrayList<>())
          .add(node);
    }
    for (List<UpgradeNode> group : nodesByPerk.values()) {
      group.sort(Comparator.comparingInt(UpgradeNode::level));
    }
    // A "maxed" prerequisite needs the target perk's converted max level; build
    // the level map before constructing any SkillNode requirements.
    Map<String, Integer> maxLevelByPerk = new HashMap<>();
    for (Map.Entry<String, List<UpgradeNode>> entry : nodesByPerk.entrySet()) {
      maxLevelByPerk.put(entry.getKey(), entry.getValue().size());
    }

    Map<String, SkillNode> skillNodes = new LinkedHashMap<>();
    for (Map.Entry<String, List<UpgradeNode>> entry : nodesByPerk.entrySet()) {
      skillNodes.put(
          entry.getKey(),
          convertPerk(legacy, entry.getKey(), entry.getValue(), perkByNodeKey, maxLevelByPerk));
    }

    String rootPerk = perkByNodeKey.get(legacy.rootNodeKey());
    String v2Root =
        rootPerk != null && skillNodes.containsKey(rootPerk)
            ? rootPerk
            : skillNodes.isEmpty() ? legacy.rootNodeKey() : skillNodes.keySet().iterator().next();

    SkillTree tree =
        new SkillTree(
            Key.key("modularjobs", "upgrade_tree/" + legacy.jobKey()),
            legacy.jobKey(),
            legacy.description(),
            legacy.skillPointsPerLevel(),
            v2Root,
            skillNodes);
    skillTreeRegistry.register(tree);
    return tree;
  }

  private SkillNode convertPerk(
      UpgradeTree legacy,
      String perk,
      List<UpgradeNode> group,
      Map<String, String> perkByNodeKey,
      Map<String, Integer> maxLevelByPerk) {
    final UpgradeNode base = group.get(0);
    boolean isRoot = Objects.equals(perk, perkByNodeKey.get(legacy.rootNodeKey()));
    SkillNodeKind kind = isRoot ? SkillNodeKind.ROOT : SkillNodeKind.SKILL;

    List<NodeLevel> levels = new ArrayList<>();
    List<NodeEffect> nodeEffects = new ArrayList<>();
    for (UpgradeNode member : group) {
      if (kind == SkillNodeKind.SKILL) {
        levels.add(new NodeLevel(member.cost(), mapEffects(member.effects())));
      } else {
        nodeEffects.addAll(mapEffects(member.effects()));
      }
    }

    LevelEffectMode mode =
        legacy.getPerkPolicy(perk) == PerkPolicy.ADDITIVE
            ? LevelEffectMode.CUMULATIVE
            : LevelEffectMode.REPLACE;

    Set<String> prerequisites = new HashSet<>();
    Set<String> excludes = new HashSet<>();
    List<Requirement> requirements = new ArrayList<>();
    Set<Requirement> seenRequirements = new HashSet<>();
    for (UpgradeNode member : group) {
      for (String prereq : member.prerequisites()) {
        String target = perkByNodeKey.get(prereq);
        if (target != null && !target.equals(perk)) {
          prerequisites.add(target);
        }
      }
      for (String excl : member.exclusive()) {
        String target = perkByNodeKey.get(excl);
        if (target != null && !target.equals(perk)) {
          excludes.add(target);
        }
      }
      for (String maxed : member.maxedPrerequisites()) {
        String target = perkByNodeKey.get(maxed);
        if (target == null || target.equals(perk)) {
          continue;
        }
        Integer targetMax = maxLevelByPerk.get(target);
        if (targetMax == null) {
          continue;
        }
        Requirement requirement = new NodeLevelRequirement(target, targetMax);
        if (seenRequirements.add(requirement)) {
          requirements.add(requirement);
        }
      }
    }

    Position position = null;
    for (UpgradeNode member : group) {
      if (member.position() != null) {
        position = member.position();
        break;
      }
    }

    return new SkillNode(
        Key.key(legacy.jobKey(), perk),
        base.name(),
        base.description(),
        base.icon(),
        base.unlockedIcon(),
        base.itemModel(),
        base.unlockedItemModel(),
        kind,
        base.cost(),
        kind == SkillNodeKind.SKILL ? levels.size() : 1,
        mode,
        levels,
        requirements,
        prerequisites,
        excludes,
        nodeEffects,
        position,
        base.pathPoints(),
        List.of());
  }

  private static List<NodeEffect> mapEffects(List<UpgradeEffect> legacyEffects) {
    List<NodeEffect> mapped = new ArrayList<>();
    for (UpgradeEffect effect : legacyEffects) {
      if (effect instanceof UpgradeEffect.BoostEffect boost) {
        // Preserve the exact BigDecimal; never round-trip through double.
        mapped.add(new NodeEffect.BoostEffect(boost.target(), boost.multiplier()));
      } else if (effect instanceof UpgradeEffect.RuledBoostEffect ruled) {
        mapped.add(new NodeEffect.RuledBoostEffect(ruled.target(), ruled.boostSource()));
      } else if (effect instanceof UpgradeEffect.PermissionEffect permission) {
        mapped.add(new NodeEffect.PermissionEffect(permission.permissions()));
      }
    }
    return mapped;
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
      String defaultJson =
          """
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
