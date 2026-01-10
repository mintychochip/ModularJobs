package net.aincraft.upgrade.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.config.BoostSourceConfig.ConditionConfig;
import net.aincraft.boost.config.ConditionConfigParser;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeEffect.BoostEffect;
import net.aincraft.upgrade.UpgradeEffect.PermissionEffect;
import net.aincraft.upgrade.UpgradeEffect.RuledBoostEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.config.UpgradeTreeConfig.EffectConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.NodeConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.PositionConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.RuleConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.BoostConfig;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;

/**
 * Parses UpgradeTreeConfig from JSON into UpgradeTree instances.
 * Supports both simple boost effects and ruled boost effects with conditions.
 */
public final class UpgradeTreeConfigParser {

  private final ConditionFactory conditionFactory;
  private final BoostFactory boostFactory;
  private final ConditionConfigParser conditionParser;
  private final net.aincraft.boost.config.BoostSourceConfigParser boostSourceParser;

  public UpgradeTreeConfigParser(
      ConditionFactory conditionFactory,
      BoostFactory boostFactory
  ) {
    this.conditionFactory = conditionFactory;
    this.boostFactory = boostFactory;
    this.conditionParser = new ConditionConfigParser(conditionFactory);
    this.boostSourceParser = new net.aincraft.boost.config.BoostSourceConfigParser(
        conditionFactory, boostFactory);
  }

  public UpgradeTree parse(UpgradeTreeConfig config) {
    String jobKey = config.job();
    Key treeKey = Key.key("modularjobs", "upgrade_tree/" + jobKey);

    Map<String, UpgradeNode> nodes = new HashMap<>();

    for (Map.Entry<String, NodeConfig> entry : config.nodes().entrySet()) {
      String nodeKey = entry.getKey();
      NodeConfig nodeConfig = entry.getValue();
      UpgradeNode node = parseNode(jobKey, nodeKey, nodeConfig);
      nodes.put(nodeKey, node);
    }

    return new UpgradeTree(
        treeKey,
        jobKey,
        config.description(),
        config.root(),
        config.skill_points_per_level(),
        nodes,
        new HashMap<>(),   // No perk policies in legacy format
        Set.of()          // No paths in legacy format
    );
  }

  private UpgradeNode parseNode(String jobKey, String nodeKey, NodeConfig config) {
    Key key = Key.key(jobKey, nodeKey);

    Material icon;
    try {
      icon = Material.valueOf(config.icon().toUpperCase());
    } catch (IllegalArgumentException e) {
      icon = Material.BARRIER; // Fallback
    }

    Set<String> prerequisites = config.prerequisites() != null
        ? new HashSet<>(config.prerequisites())
        : Set.of();

    Set<String> exclusive = config.exclusive() != null
        ? new HashSet<>(config.exclusive())
        : Set.of();

    List<String> children = config.children() != null
        ? config.children()
        : List.of();

    List<UpgradeEffect> effects = new ArrayList<>();
    if (config.effects() != null) {
      for (EffectConfig effectConfig : config.effects()) {
        UpgradeEffect effect = parseEffect(effectConfig, jobKey, nodeKey);
        if (effect != null) {
          effects.add(effect);
        }
      }
    }

    Position position = null;
    if (config.position() != null) {
      PositionConfig pos = config.position();
      position = new Position(pos.x(), pos.y());
    }

    // Auto-detect perkId and level from node key if not explicitly set
    String perkId = config.perkId();
    int level = config.level() != null ? config.level() : 1;

    if (perkId == null) {
      // Try parsing from nodeKey pattern: "name_level" (e.g., "efficiency_1", "crit_chance_2")
      String[] parts = nodeKey.split("_");
      if (parts.length >= 2) {
        String lastPart = parts[parts.length - 1];
        try {
          int parsedLevel = Integer.parseInt(lastPart);
          // Successfully parsed level - extract perkId from remaining parts
          level = parsedLevel;
          perkId = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
        } catch (NumberFormatException ignored) {
          // No numeric suffix - use nodeKey as perkId with level 1
          perkId = nodeKey;
          level = 1;
        }
      } else {
        // No underscore - use nodeKey as perkId with level 1
        perkId = nodeKey;
        level = 1;
      }
    }

    return new UpgradeNode(
        key,
        config.name(),
        config.description(),
        icon,           // locked icon
        icon,           // unlocked icon (same as locked for legacy format)
        null,           // itemModel (not supported in legacy)
        null,           // unlockedItemModel (not supported in legacy)
        config.cost(),
        prerequisites,
        exclusive,
        children,
        effects,
        position,
        List.of(), // pathPoints - empty for legacy format
        perkId,
        level
    );
  }

  private UpgradeEffect parseEffect(EffectConfig config, String jobKey, String nodeKey) {
    return switch (config.type().toLowerCase()) {
      case "boost" -> {
        String target = config.target() != null ? config.target() : BoostEffect.TARGET_ALL;
        BigDecimal amount = config.amount() != null
            ? BigDecimal.valueOf(config.amount())
            : BigDecimal.ONE;
        yield new BoostEffect(target, amount);
      }
      case "ruled_boost" -> {
        // Parse ruled boost with conditions (same structure as boost_sources)
        String target = config.target() != null ? config.target() : BoostEffect.TARGET_ALL;
        BoostSource boostSource = parseRuledBoostSource(config, jobKey, nodeKey);
        yield new RuledBoostEffect(target, boostSource);
      }
      case "permission" -> {
        String perm = config.permission() != null ? config.permission() : "jobs.unknown";
        yield new PermissionEffect(perm);
      }
      default -> null;
    };
  }

  private BoostSource parseRuledBoostSource(EffectConfig config, String jobKey, String nodeKey) {
    Key key = Key.key("modularjobs", "upgrade/" + jobKey + "/" + nodeKey);
    List<Rule> rules = new ArrayList<>();

    if (config.rules() != null) {
      for (RuleConfig ruleConfig : config.rules()) {
        Rule rule = boostSourceParser.parseRule(ruleConfig);
        rules.add(rule);
      }
    }

    String desc = config.description() != null ? config.description() : nodeKey + " upgrade boost";
    return new RuledBoostSourceImpl(rules, key, desc);
  }
}
