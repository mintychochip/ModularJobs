package dev.mintychochip.upgrade.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.upgrade.NodeEffect;
import dev.mintychochip.upgrade.NodeLevel;
import dev.mintychochip.upgrade.NodeStateWrite;
import dev.mintychochip.upgrade.Position;
import dev.mintychochip.upgrade.Requirement;
import dev.mintychochip.upgrade.SkillNode;
import dev.mintychochip.upgrade.SkillNode.LevelEffectMode;
import dev.mintychochip.upgrade.SkillNodeKind;
import dev.mintychochip.upgrade.SkillTree;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Parses version-2 skill tree JSON into SkillTree instances. Validates node
 * keys, requirement/effect vocabulary, and prerequisite/exclude references.
 */
public final class SkillTreeConfigParser {

  private static final int VERSION = 2;

  private final SkillTreeRequirementParser requirementParser;
  private final SkillTreeEffectParser effectParser;

  public SkillTreeConfigParser(BoostFactory boostFactory, ConditionFactory conditionFactory) {
    this.requirementParser = new SkillTreeRequirementParser();
    this.effectParser = new SkillTreeEffectParser(boostFactory, conditionFactory);
  }

  @NotNull
  public SkillTree parse(@NotNull JsonObject root) {
    if (!root.has("version") || root.get("version").getAsInt() != VERSION) {
      throw new IllegalArgumentException("Skill tree must declare \"version\": 2");
    }
    if (!root.has("job") || !root.has("root") || !root.has("nodes")) {
      throw new IllegalArgumentException("Skill tree requires \"job\", \"root\", and \"nodes\"");
    }

    final String jobKey = root.get("job").getAsString();
    final String rootNodeKey = root.get("root").getAsString();
    final int pointsPerLevel = root.has("skill_points_per_level")
        ? root.get("skill_points_per_level").getAsInt() : 1;
    final String description = root.has("description") && !root.get("description").isJsonNull()
        ? root.get("description").getAsString() : null;

    JsonObject nodesObj = root.getAsJsonObject("nodes");
    Map<String, SkillNode> nodes = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
      nodes.put(entry.getKey(), parseNode(jobKey, entry.getKey(), entry.getValue().getAsJsonObject()));
    }

    validateReferences(jobKey, rootNodeKey, nodes);
    validateStateWriteConflicts(nodes);

    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/" + jobKey),
        jobKey, description, pointsPerLevel, rootNodeKey, nodes);
  }

  private SkillNode parseNode(String jobKey, String nodeKey, JsonObject obj) {
    final SkillNodeKind kind = SkillNodeKind.valueOf(obj.get("kind").getAsString().toUpperCase());
    final String name = obj.get("name").getAsString();
    final String description = obj.has("description") && !obj.get("description").isJsonNull()
        ? obj.get("description").getAsString() : null;

    final Set<String> prerequisites = parseStringSet(obj, "prerequisites");
    final Set<String> excludes = parseStringSet(obj, "excludes");
    final int cost = obj.has("cost") ? obj.get("cost").getAsInt() : 0;

    List<Requirement> requirements = new ArrayList<>();
    if (obj.has("requirements")) {
      requirements.add(requirementParser.parse(obj.get("requirements")));
    }

    List<NodeEffect> effects = new ArrayList<>();
    List<NodeLevel> levels = new ArrayList<>();
    if (obj.has("levels")) {
      JsonArray levelsArray = obj.getAsJsonArray("levels");
      for (JsonElement levelEl : levelsArray) {
        JsonObject levelObj = levelEl.getAsJsonObject();
        int levelCost = levelObj.has("cost") ? levelObj.get("cost").getAsInt() : 0;
        List<NodeEffect> levelEffects = new ArrayList<>();
        if (levelObj.has("effects")) {
          for (JsonElement effectEl : levelObj.getAsJsonArray("effects")) {
            NodeEffect effect = effectParser.parse(effectEl);
            validateEffect(kind, nodeKey, effect);
            levelEffects.add(effect);
          }
        }
        levels.add(new NodeLevel(levelCost, levelEffects));
      }
    } else if (obj.has("effects")) {
      for (JsonElement effectEl : obj.getAsJsonArray("effects")) {
        NodeEffect effect = effectParser.parse(effectEl);
        validateEffect(kind, nodeKey, effect);
        effects.add(effect);
      }
    }

    LevelEffectMode mode = LevelEffectMode.REPLACE;
    if (obj.has("level_effect_mode") && "cumulative".equalsIgnoreCase(obj.get("level_effect_mode").getAsString())) {
      mode = LevelEffectMode.CUMULATIVE;
    }

    List<NodeStateWrite> stateWrites = new ArrayList<>();
    if (obj.has("state")) {
      for (JsonElement stateEl : obj.getAsJsonArray("state")) {
        JsonObject stateObj = stateEl.getAsJsonObject();
        if (stateObj.has("set")) {
          JsonObject setObj = stateObj.getAsJsonObject("set");
          setObj.entrySet().forEach(e -> stateWrites.add(new NodeStateWrite(
              NodeStateWrite.Op.SET, parseKey(e.getKey()), e.getValue().getAsString())));
        } else if (stateObj.has("remove")) {
          JsonObject removeObj = stateObj.getAsJsonObject("remove");
          removeObj.entrySet().forEach(e -> stateWrites.add(new NodeStateWrite(
              NodeStateWrite.Op.REMOVE, parseKey(e.getKey()), "")));
        }
      }
    }
    if (!stateWrites.isEmpty() && kind != SkillNodeKind.MAJOR) {
      throw new IllegalArgumentException("Only major nodes may define state writes: " + nodeKey);
    }

    String icon = "DIAMOND";
    if (obj.has("icon") && !obj.get("icon").isJsonNull()) {
      String raw = obj.get("icon").getAsString().trim();
      if (!raw.isEmpty()) {
        icon = raw.contains(":")
            ? raw.substring(raw.indexOf(':') + 1).toUpperCase()
            : raw.toUpperCase();
      }
    }

    Position position = null;
    if (obj.has("position")) {
      JsonObject posObj = obj.getAsJsonObject("position");
      position = new Position(posObj.get("x").getAsInt(), posObj.get("y").getAsInt());
    }

    return new SkillNode(
        Key.key(jobKey, nodeKey), name, description,
        icon, icon, null, null,
        kind, cost,
        kind == SkillNodeKind.SKILL ? levels.size() : 1,
        mode, levels, requirements, prerequisites, excludes, effects,
        position, List.of(), stateWrites);
  }

  private void validateEffect(SkillNodeKind kind, String nodeKey, NodeEffect effect) {
    if (kind != SkillNodeKind.MAJOR && effect instanceof NodeEffect.StateSetEffect) {
      throw new IllegalArgumentException("Only major nodes may define state-setting effects: " + nodeKey);
    }
  }

  private void validateStateWriteConflicts(Map<String, SkillNode> nodes) {
    List<SkillNode> majors = nodes.values().stream()
        .filter(SkillNode::isMajor)
        .toList();
    for (int i = 0; i < majors.size(); i++) {
      SkillNode left = majors.get(i);
      for (int j = i + 1; j < majors.size(); j++) {
        SkillNode right = majors.get(j);
        if (left.excludes().contains(right.key().value())
            || right.excludes().contains(left.key().value())) {
          continue;
        }
        Map<Key, NodeStateWrite> leftWrites = new HashMap<>();
        for (NodeStateWrite write : left.stateWrites()) {
          leftWrites.put(write.key(), write);
        }
        for (NodeStateWrite write : right.stateWrites()) {
          NodeStateWrite leftWrite = leftWrites.get(write.key());
          if (leftWrite != null
              && (leftWrite.op() != write.op() || !leftWrite.value().equals(write.value()))) {
            throw new IllegalArgumentException(
                "Non-exclusive majors write conflicting state key: " + write.key());
          }
        }
      }
    }
  }

  private void validateReferences(String jobKey, String rootNodeKey, Map<String, SkillNode> nodes) {
    if (!nodes.containsKey(rootNodeKey)) {
      throw new IllegalArgumentException("Root node '" + rootNodeKey + "' not found in tree " + jobKey);
    }
    for (SkillNode node : nodes.values()) {
      String nodeKey = node.key().value();
      for (String prereq : node.prerequisites()) {
        if (!nodes.containsKey(prereq)) {
          throw new IllegalArgumentException("Node '" + nodeKey + "' has unknown prerequisite '" + prereq + "'");
        }
      }
      for (String excluded : node.excludes()) {
        if (!nodes.containsKey(excluded)) {
          throw new IllegalArgumentException("Node '" + nodeKey + "' has unknown exclude '" + excluded + "'");
        }
      }
    }
  }

  private Set<String> parseStringSet(JsonObject obj, String field) {
    Set<String> result = new HashSet<>();
    if (obj.has(field)) {
      for (JsonElement el : obj.getAsJsonArray(field)) {
        result.add(el.getAsString());
      }
    }
    return result;
  }

  private Key parseKey(String raw) {
    int separator = raw.indexOf(':');
    if (separator > 0) {
      return Key.key(raw.substring(0, separator), raw.substring(separator + 1));
    }
    int dot = raw.indexOf('.');
    if (dot > 0) {
      return Key.key(raw.substring(0, dot), raw.substring(dot + 1));
    }
    throw new IllegalArgumentException("Namespaced key must use namespace.key or namespace:key: " + raw);
  }
}
