package net.aincraft.upgrade.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.upgrade.PerkPolicy;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.wynncraft.AbilityMeta;
import net.aincraft.upgrade.wynncraft.AbilityMeta.BoostConfig;
import net.aincraft.upgrade.wynncraft.AbilityMeta.EffectConfig;
import net.aincraft.upgrade.wynncraft.AbilityMeta.PolicyConfig;
import net.aincraft.upgrade.wynncraft.AbilityMeta.RuleConfig;
import net.aincraft.upgrade.wynncraft.Archetype;
import net.aincraft.upgrade.wynncraft.ConnectorMeta;
import net.aincraft.upgrade.wynncraft.IconConfig;
import net.aincraft.upgrade.wynncraft.LayoutItem;
import net.aincraft.upgrade.wynncraft.LayoutItemType;
import net.aincraft.upgrade.wynncraft.WynncraftTreeConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom Gson deserializer for WynncraftTreeConfig.
 * Handles the polymorphic meta field in LayoutItem.
 */
public final class WynncraftTreeConfigDeserializer implements JsonDeserializer<WynncraftTreeConfig> {

  @Override
  public WynncraftTreeConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    JsonObject root = json.getAsJsonObject();

    String treeId = getString(root, "tree_id");

    // Parse metadata for job info
    String job = null;
    int skillPointsPerLevel = 1;
    String rootId = null;

    if (root.has("metadata")) {
      JsonObject metadata = root.getAsJsonObject("metadata");
      job = getString(metadata, "job");
      skillPointsPerLevel = metadata.has("skill_points_per_level") ? metadata.get("skill_points_per_level").getAsInt() : 1;
      rootId = getString(metadata, "root");
    } else {
      // Fallback to top-level fields
      job = getString(root, "job");
      skillPointsPerLevel = root.has("skill_points_per_level") ? root.get("skill_points_per_level").getAsInt() : 1;
      rootId = getString(root, "root");
    }

    String displayName = job; // Default to job name

    // Parse archetypes
    List<Archetype> archetypes = new ArrayList<>();
    if (root.has("archetypes")) {
      for (JsonElement elem : root.getAsJsonArray("archetypes")) {
        JsonObject archObj = elem.getAsJsonObject();
        archetypes.add(new Archetype(
            getString(archObj, "id"),
            getString(archObj, "name"),
            getString(archObj, "color")
        ));
      }
    }

    // Parse layout items
    List<LayoutItem> layout = new ArrayList<>();
    if (root.has("layout")) {
      for (JsonElement elem : root.getAsJsonArray("layout")) {
        JsonObject itemObj = elem.getAsJsonObject();
        layout.add(deserializeLayoutItem(itemObj, context));
      }
    }

    // Parse perk policies
    Map<String, PerkPolicy> perkPolicies = new HashMap<>();
    if (root.has("perk_policies")) {
      JsonObject policiesObj = root.getAsJsonObject("perk_policies");
      for (Map.Entry<String, JsonElement> entry : policiesObj.entrySet()) {
        String policyStr = entry.getValue().getAsString();
        perkPolicies.put(entry.getKey(), PerkPolicy.valueOf(policyStr.toUpperCase()));
      }
    }

    return new WynncraftTreeConfig(treeId, displayName, job, skillPointsPerLevel, rootId, archetypes, layout, perkPolicies);
  }

  private LayoutItem deserializeLayoutItem(JsonObject itemObj, JsonDeserializationContext context) {
    String id = getString(itemObj, "id");
    String typeStr = getString(itemObj, "type");
    LayoutItemType type = LayoutItemType.valueOf(typeStr.toUpperCase());

    // Parse coordinates
    JsonObject coordsObj = itemObj.getAsJsonObject("coordinates");
    Position coordinates = new Position(
        coordsObj.get("x").getAsInt(),
        coordsObj.get("y").getAsInt()
    );

    // Parse optional archetype_refs (array) or archetype_ref (string)
    String archetypeRef = null;
    if (itemObj.has("archetype_refs")) {
      // Take first archetype ref if multiple
      JsonArray refsArray = itemObj.getAsJsonArray("archetype_refs");
      if (refsArray.size() > 0) {
        archetypeRef = refsArray.get(0).getAsString();
      }
    } else if (itemObj.has("archetype_ref")) {
      archetypeRef = getString(itemObj, "archetype_ref");
    }

    // Parse optional family
    List<String> family = null;
    if (itemObj.has("family")) {
      family = new ArrayList<>();
      for (JsonElement elem : itemObj.getAsJsonArray("family")) {
        family.add(elem.getAsString());
      }
    }

    // Parse meta based on type
    var meta = itemObj.getAsJsonObject("meta");
    Object metaObj;

    if (type == LayoutItemType.ABILITY) {
      metaObj = deserializeAbilityMeta(meta);
    } else if (type == LayoutItemType.CONNECTOR) {
      metaObj = deserializeConnectorMeta(meta);
    } else {
      throw new IllegalArgumentException("Unknown layout item type: " + type);
    }

    return new LayoutItem(id, type, coordinates, (net.aincraft.upgrade.wynncraft.LayoutItemMeta) metaObj, archetypeRef, family);
  }

  private AbilityMeta deserializeAbilityMeta(JsonObject meta) {
    String name = getString(meta, "name");

    // Parse icon
    JsonObject iconObj = meta.getAsJsonObject("icon");
    IconConfig icon = new IconConfig(
        getString(iconObj, "id"),
        iconObj.has("custom_model_data") ? iconObj.get("custom_model_data").getAsInt() : 0
    );

    int cost = meta.has("cost") ? meta.get("cost").getAsInt() : 0;
    boolean required = meta.has("required") && meta.get("required").getAsBoolean();
    boolean major = meta.has("major") && meta.get("major").getAsBoolean();

    // Parse description
    List<String> description = new ArrayList<>();
    if (meta.has("description")) {
      for (JsonElement elem : meta.getAsJsonArray("description")) {
        description.add(elem.getAsString());
      }
    }

    // Parse prerequisites
    List<String> prerequisites = new ArrayList<>();
    if (meta.has("prerequisites")) {
      for (JsonElement elem : meta.getAsJsonArray("prerequisites")) {
        prerequisites.add(elem.getAsString());
      }
    }

    // Parse exclusive_with
    List<String> exclusiveWith = new ArrayList<>();
    if (meta.has("exclusive_with")) {
      for (JsonElement elem : meta.getAsJsonArray("exclusive_with")) {
        exclusiveWith.add(elem.getAsString());
      }
    }

    // Parse effects
    List<EffectConfig> effects = new ArrayList<>();
    if (meta.has("effects")) {
      for (JsonElement elem : meta.getAsJsonArray("effects")) {
        effects.add(deserializeEffectConfig(elem.getAsJsonObject()));
      }
    }

    // Parse path_from_parent - optional list of path points from parent to this node
    List<Position> pathFromParent = null;
    if (meta.has("path_from_parent")) {
      pathFromParent = new ArrayList<>();
      for (JsonElement elem : meta.getAsJsonArray("path_from_parent")) {
        JsonObject posObj = elem.getAsJsonObject();
        Position pos = new Position(
            posObj.get("x").getAsInt(),
            posObj.get("y").getAsInt()
        );
        pathFromParent.add(pos);
      }
    }

    // Parse optional perk_id for leveled perks (e.g., "crit_chance")
    String perkId = null;
    if (meta.has("perk_id")) {
      perkId = meta.get("perk_id").getAsString();
    }

    // Parse optional level for leveled perks (1, 2, 3...)
    Integer level = null;
    if (meta.has("level")) {
      level = meta.get("level").getAsInt();
    }

    return new AbilityMeta(name, icon, cost, description, prerequisites, exclusiveWith, effects, required, major, pathFromParent, perkId, level);
  }

  private ConnectorMeta deserializeConnectorMeta(JsonObject meta) {
    // Parse links
    List<String> links = new ArrayList<>();
    if (meta.has("links")) {
      for (JsonElement elem : meta.getAsJsonArray("links")) {
        links.add(elem.getAsString());
      }
    }

    // Parse icon shapes (supports both "icon" and "shapes" field names)
    JsonObject iconObj = null;
    if (meta.has("icon")) {
      iconObj = meta.getAsJsonObject("icon");
    } else if (meta.has("shapes")) {
      iconObj = meta.getAsJsonObject("shapes");
    } else {
      throw new IllegalArgumentException("Connector meta must have 'icon' or 'shapes' field");
    }

    JsonObject lockedObj = iconObj.getAsJsonObject("locked");
    JsonObject unlockedObj = iconObj.getAsJsonObject("unlocked");

    IconConfig lockedIcon = new IconConfig(
        getString(lockedObj, "id"),
        lockedObj.has("custom_model_data") ? lockedObj.get("custom_model_data").getAsInt() : 0
    );

    IconConfig unlockedIcon = new IconConfig(
        getString(unlockedObj, "id"),
        unlockedObj.has("custom_model_data") ? unlockedObj.get("custom_model_data").getAsInt() : 0
    );

    return new ConnectorMeta(links, new ConnectorMeta.ConnectorShapes(lockedIcon, unlockedIcon));
  }

  private EffectConfig deserializeEffectConfig(JsonObject effectObj) {
    // Parse policy for ruled_boost effects
    PolicyConfig policy = null;
    if (effectObj.has("policy")) {
      JsonObject policyObj = effectObj.getAsJsonObject("policy");
      policy = new PolicyConfig(
          getString(policyObj, "type"),
          policyObj.has("topK") ? policyObj.get("topK").getAsInt() : null
      );
    }

    // Parse rules for ruled_boost effects
    List<RuleConfig> rules = null;
    if (effectObj.has("rules")) {
      rules = new ArrayList<>();
      for (JsonElement ruleElem : effectObj.getAsJsonArray("rules")) {
        JsonObject ruleObj = ruleElem.getAsJsonObject();

        // Parse conditions as a raw object (will be parsed later)
        Object conditions = ruleObj.get("conditions");

        // Parse boost
        JsonObject boostObj = ruleObj.getAsJsonObject("boost");
        BoostConfig boost = new BoostConfig(
            getString(boostObj, "type"),
            boostObj.get("amount").getAsDouble()
        );

        rules.add(new RuleConfig(
            ruleObj.get("priority").getAsInt(),
            conditions,
            boost
        ));
      }
    }

    return new EffectConfig(
        getString(effectObj, "type"),
        effectObj.has("target") ? getString(effectObj, "target") : null,
        effectObj.has("amount") ? effectObj.get("amount").getAsDouble() : null,
        effectObj.has("ability") ? getString(effectObj, "ability") : null,
        effectObj.has("description") ? getString(effectObj, "description") : null,
        effectObj.has("stat") ? getString(effectObj, "stat") : null,
        effectObj.has("value") ? effectObj.get("value").getAsInt() : null,
        effectObj.has("unlock_type") ? getString(effectObj, "unlock_type") : null,
        effectObj.has("unlock_key") ? getString(effectObj, "unlock_key") : null,
        effectObj.has("permission") ? getString(effectObj, "permission") : null,
        policy,
        rules
    );
  }

  private String getString(JsonObject obj, String memberName) {
    if (!obj.has(memberName)) {
      throw new IllegalArgumentException("Missing required field: " + memberName);
    }
    return obj.get(memberName).getAsString();
  }
}
