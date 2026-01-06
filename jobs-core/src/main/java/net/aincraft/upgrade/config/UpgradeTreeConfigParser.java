package net.aincraft.upgrade.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeEffect.BoostEffect;
import net.aincraft.upgrade.UpgradeEffect.PassiveEffect;
import net.aincraft.upgrade.UpgradeEffect.StatEffect;
import net.aincraft.upgrade.UpgradeEffect.UnlockEffect;
import net.aincraft.upgrade.UpgradeNode;
import net.aincraft.upgrade.UpgradeNode.NodeType;
import net.aincraft.upgrade.UpgradeNode.Position;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.config.UpgradeTreeConfig.EffectConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.NodeConfig;
import net.aincraft.upgrade.config.UpgradeTreeConfig.PositionConfig;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;

/**
 * Parses UpgradeTreeConfig from JSON into UpgradeTree instances.
 */
public final class UpgradeTreeConfigParser {

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
        config.root(),
        config.skill_points_per_level(),
        nodes
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

    NodeType nodeType = switch (config.resolvedType().toLowerCase()) {
      case "major", "specialization" -> NodeType.MAJOR;
      default -> NodeType.MINOR;
    };

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
        UpgradeEffect effect = parseEffect(effectConfig);
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

    return new UpgradeNode(
        key,
        config.name(),
        config.description(),
        icon,
        config.cost(),
        nodeType,
        prerequisites,
        exclusive,
        children,
        effects,
        position
    );
  }

  private UpgradeEffect parseEffect(EffectConfig config) {
    return switch (config.type().toLowerCase()) {
      case "boost" -> {
        String target = config.target() != null ? config.target() : BoostEffect.TARGET_ALL;
        BigDecimal amount = config.amount() != null
            ? BigDecimal.valueOf(config.amount())
            : BigDecimal.ONE;
        yield new BoostEffect(target, amount);
      }
      case "passive" -> {
        String ability = config.ability() != null ? config.ability() : "unknown";
        String desc = config.description() != null ? config.description() : "";
        yield new PassiveEffect(ability, desc);
      }
      case "stat" -> {
        String stat = config.stat() != null ? config.stat() : "unknown";
        int value = config.value() != null ? config.value() : 0;
        yield new StatEffect(stat, value);
      }
      case "unlock" -> {
        String unlockType = config.unlock_type() != null ? config.unlock_type() : UnlockEffect.UNLOCK_FEATURE;
        String unlockKey = config.unlock_key() != null ? config.unlock_key() : "unknown";
        yield new UnlockEffect(unlockType, unlockKey);
      }
      default -> null;
    };
  }
}
