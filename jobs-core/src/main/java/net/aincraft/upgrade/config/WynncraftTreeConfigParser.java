package net.aincraft.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.config.BoostSourceConfig;
import net.aincraft.boost.config.ConditionConfigParser;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.PerkPolicy;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeEffect.BoostEffect;
import net.aincraft.upgrade.UpgradeEffect.PermissionEffect;
import net.aincraft.upgrade.UpgradeEffect.RuledBoostEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.wynncraft.AbilityMeta;
import net.aincraft.upgrade.wynncraft.AbilityMeta.EffectConfig;
import net.aincraft.upgrade.wynncraft.AbilityMeta.RuleConfig;
import net.aincraft.upgrade.wynncraft.AbilityMeta.BoostConfig;
import net.aincraft.upgrade.wynncraft.LayoutItem;
import net.aincraft.upgrade.wynncraft.LayoutItemType;
import net.aincraft.upgrade.wynncraft.WynncraftTreeConfig;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;

/**
 * Parses WynncraftTreeConfig into UpgradeTree instances.
 * Supports the new Wynncraft-style JSON format with archetypes and explicit positioning.
 */
public final class WynncraftTreeConfigParser {

  private static final int DEFAULT_SKILL_POINTS_PER_LEVEL = 1;

  private final BoostFactory boostFactory;
  private final ConditionConfigParser conditionParser;
  private final Gson gson;

  public WynncraftTreeConfigParser(
      ConditionFactory conditionFactory,
      BoostFactory boostFactory
  ) {
    this.boostFactory = boostFactory;
    this.conditionParser = new ConditionConfigParser(conditionFactory);
    this.gson = new Gson();
  }

  /**
   * Parse a Wynncraft-style tree config into an UpgradeTree.
   *
   * @param config the parsed JSON configuration
   * @return a fully constructed UpgradeTree
   */
  public UpgradeTree parse(WynncraftTreeConfig config) {
    String jobKey = config.job();  // Use job field
    Key treeKey = Key.key("modularjobs", "upgrade_tree/" + jobKey);

    Map<String, UpgradeNode> nodes = new HashMap<>();
    String rootNodeKey = config.root();  // Use root from config

    // Parse all ability nodes
    for (LayoutItem item : config.layout()) {
      if (item.type() == LayoutItemType.ABILITY) {
        AbilityMeta meta = item.abilityMeta().orElseThrow();
        UpgradeNode node = parseAbilityNode(jobKey, item.id(), item.coordinates(), meta);
        nodes.put(item.id(), node);
      }
    }

    // Populate children field by reversing prerequisites
    for (UpgradeNode node : nodes.values()) {
      for (String prereq : node.prerequisites()) {
        UpgradeNode parent = nodes.get(prereq);
        if (parent != null) {
          // Add this node as a child of its prerequisite
          List<String> updatedChildren = new ArrayList<>(parent.children());
          updatedChildren.add(getShortKey(node));
          nodes.put(prereq, new UpgradeNode(
              parent.key(),
              parent.name(),
              parent.description(),
              parent.icon(),
              parent.unlockedIcon(),
              parent.itemModel(),
              parent.unlockedItemModel(),
              parent.cost(),
              parent.prerequisites(),
              parent.exclusive(),
              updatedChildren,
              parent.effects(),
              parent.position(),
              parent.pathPoints(),
              parent.perkId(),
              parent.level()
          ));
        }
      }
    }

    if (rootNodeKey == null) {
      throw new IllegalArgumentException("No root node found in tree " + jobKey);
    }

    // Get perk policies from config, default to empty map if not specified
    Map<String, PerkPolicy> perkPolicies = config.perkPolicies();
    if (perkPolicies == null) {
      perkPolicies = new HashMap<>();
    }

    // Get paths from config, default to empty set
    Set<Position> paths = config.paths() != null ? new HashSet<>(config.paths()) : Set.of();

    return new UpgradeTree(
        treeKey,
        jobKey,
        config.description(),
        rootNodeKey,
        config.skillPointsPerLevel(),
        nodes,
        perkPolicies,
        paths
    );
  }

  /**
   * Parse an ability layout item into an UpgradeNode.
   */
  private UpgradeNode parseAbilityNode(String jobKey, String nodeId, Position position, AbilityMeta meta) {
    Key key = Key.key(jobKey, nodeId);

    // Extract locked and unlocked icons
    Material lockedIcon = meta.icons().locked().toMaterial();
    Material unlockedIcon = meta.icons().unlocked().toMaterial();
    String lockedItemModel = meta.icons().locked().itemModel();
    String unlockedItemModel = meta.icons().unlocked().itemModel();

    Set<String> prerequisites = new HashSet<>(meta.prerequisites());
    Set<String> exclusive = new HashSet<>(meta.exclusiveWith());
    List<String> children = List.of(); // Children will be determined by layout

    List<UpgradeEffect> effects = new ArrayList<>();
    for (EffectConfig effectConfig : meta.effects()) {
      UpgradeEffect effect = parseEffect(effectConfig);
      if (effect != null) {
        effects.add(effect);
      }
    }

    // Combine description lines into single string
    String description = meta.combinedDescription();

    // Auto-detect perkId and level from node ID if not explicitly set
    String perkId = meta.perkId();
    int level = meta.level() != null ? meta.level() : 1;

    if (perkId == null) {
      // Try parsing from nodeId pattern: "name_level" (e.g., "efficiency_1", "crit_chance_2")
      String[] parts = nodeId.split("_");
      if (parts.length >= 2) {
        String lastPart = parts[parts.length - 1];
        try {
          int parsedLevel = Integer.parseInt(lastPart);
          // Successfully parsed level - extract perkId from remaining parts
          level = parsedLevel;
          perkId = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
        } catch (NumberFormatException ignored) {
          // No numeric suffix - use nodeId as perkId with level 1
          perkId = nodeId;
          level = 1;
        }
      } else {
        // No underscore - use nodeId as perkId with level 1
        perkId = nodeId;
        level = 1;
      }
    }

    return new UpgradeNode(
        key,
        meta.name(),
        description,
        lockedIcon,
        unlockedIcon,
        lockedItemModel,
        unlockedItemModel,
        meta.cost(),
        prerequisites,
        exclusive,
        children,
        effects,
        position,
        List.of(), // paths are now at tree level, not per-node
        perkId,
        level
    );
  }

  /**
   * Extract the short key (without namespace) from an UpgradeNode.
   */
  private String getShortKey(UpgradeNode node) {
    String full = node.key().asString();
    int colonIndex = full.indexOf(':');
    return colonIndex >= 0 ? full.substring(colonIndex + 1) : full;
  }

  /**
   * Parse an effect configuration into an UpgradeEffect.
   */
  private UpgradeEffect parseEffect(EffectConfig config) {
    return switch (config.type().toLowerCase()) {
      case "boost" -> {
        String target = config.target() != null ? config.target() : BoostEffect.TARGET_ALL;
        BigDecimal amount = config.amount() != null
            ? BigDecimal.valueOf(config.amount())
            : BigDecimal.ONE;
        yield new BoostEffect(target, amount);
      }
      case "permission" -> {
        // Prefer permissions array if available, otherwise fall back to single permission
        if (config.permissions() != null && !config.permissions().isEmpty()) {
          yield new PermissionEffect(config.permissions());
        } else {
          String perm = config.permission() != null ? config.permission() : "jobs.unknown";
          yield new PermissionEffect(perm);
        }
      }
      case "ruled_boost" -> {
        String target = config.target() != null ? config.target() : BoostEffect.TARGET_ALL;
        String description = config.description() != null ? config.description() : "Conditional boost";
        BoostSource boostSource = parseRuledBoostSource(config, target, description);
        yield new RuledBoostEffect(target, boostSource);
      }
      default -> null;
    };
  }

  /**
   * Parse a ruled_boost effect configuration into a RuledBoostSource.
   */
  private BoostSource parseRuledBoostSource(EffectConfig config, String target, String description) {
    // Parse rules
    List<Rule> rules = new ArrayList<>();
    if (config.rules() != null) {
      for (RuleConfig ruleConfig : config.rules()) {
        Rule rule = parseRule(ruleConfig);
        if (rule != null) {
          rules.add(rule);
        }
      }
    }

    Key key = Key.key("modularjobs", "upgrade_boost/" + target);
    return new RuledBoostSourceImpl(rules, key, description);
  }

  /**
   * Parse a single rule configuration.
   */
  private Rule parseRule(RuleConfig ruleConfig) {
    int priority = ruleConfig.priority();

    // Parse condition - conditions can be a JsonElement when deserialized
    Condition condition = parseCondition(ruleConfig.conditions());

    // Parse boost
    Boost boost = parseBoost(ruleConfig.boost());

    return new Rule(condition, priority, boost);
  }

  /**
   * Parse condition from object (could be JsonElement, JsonObject or Map).
   */
  private Condition parseCondition(Object conditionsObj) {
    if (conditionsObj == null) {
      // Return always-true condition
      return context -> true;
    }

    // Convert to BoostSourceConfig.ConditionConfig for parsing
    JsonObject conditionJson;
    if (conditionsObj instanceof JsonElement jsonElem) {
      conditionJson = jsonElem.getAsJsonObject();
    } else if (conditionsObj instanceof Map) {
      conditionJson = gson.toJsonTree(conditionsObj).getAsJsonObject();
    } else {
      conditionJson = gson.toJsonTree(conditionsObj).getAsJsonObject();
    }

    // Convert JsonObject to ConditionConfig
    BoostSourceConfig.ConditionConfig conditionConfig = gson.fromJson(conditionJson, BoostSourceConfig.ConditionConfig.class);

    return conditionParser.parse(conditionConfig);
  }

  /**
   * Parse boost configuration.
   */
  private Boost parseBoost(BoostConfig boostConfig) {
    if (boostConfig == null) {
      return boostFactory.multiplicative(BigDecimal.ONE);
    }

    String type = boostConfig.type();
    BigDecimal amount = BigDecimal.valueOf(boostConfig.amount());

    return switch (type.toLowerCase()) {
      case "additive" -> boostFactory.additive(amount);
      case "multiplicative" -> boostFactory.multiplicative(amount);
      default -> boostFactory.multiplicative(amount);
    };
  }
}
