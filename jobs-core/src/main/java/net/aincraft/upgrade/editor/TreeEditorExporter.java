package net.aincraft.upgrade.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.aincraft.upgrade.Position;
import org.jetbrains.annotations.NotNull;

/**
 * Exports EditorTree to JSON in the tree format.
 */
public final class TreeEditorExporter {

  private final Gson gson;

  @Inject
  public TreeEditorExporter() {
    this.gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
  }

  /**
   * Export the tree to JSON in tree format.
   */
  public String export(@NotNull EditorTree tree) {
    JsonObject root = new JsonObject();
    JsonObject treeObj = exportTree(tree);

    // Wrap in upgrade_trees object
    JsonObject upgradeTrees = new JsonObject();
    upgradeTrees.add(tree.jobKey(), treeObj);
    root.add("upgrade_trees", upgradeTrees);

    return gson.toJson(root);
  }

  /**
   * Export a single tree to JSON.
   */
  public String exportSingle(@NotNull EditorTree tree) {
    return gson.toJson(exportTree(tree));
  }

  private JsonObject exportTree(EditorTree tree) {
    JsonObject obj = new JsonObject();

    obj.addProperty("tree_id", tree.treeId());
    obj.addProperty("display_name", tree.displayName());
    obj.addProperty("job", tree.jobKey());
    obj.addProperty("skill_points_per_level", tree.skillPointsPerLevel());
    obj.addProperty("root", tree.rootNodeId());

    // Archetypes
    JsonArray archetypes = new JsonArray();
    for (EditorTree.EditorArchetype archetype : tree.archetypes()) {
      JsonObject arch = new JsonObject();
      arch.addProperty("id", archetype.id());
      arch.addProperty("name", archetype.name());
      arch.addProperty("color", archetype.color());
      archetypes.add(arch);
    }
    obj.add("archetypes", archetypes);

    // Perk policies
    if (!tree.perkPolicies().isEmpty()) {
      JsonObject policies = new JsonObject();
      tree.perkPolicies().forEach(policies::addProperty);
      obj.add("perk_policies", policies);
    }

    // Paths (tree-level path coordinates)
    if (!tree.paths().isEmpty()) {
      JsonArray paths = new JsonArray();
      for (Position p : tree.paths()) {
        JsonObject point = new JsonObject();
        point.addProperty("x", p.x());
        point.addProperty("y", p.y());
        paths.add(point);
      }
      obj.add("paths", paths);
    }

    // Layout (nodes in tree format)
    JsonArray layout = new JsonArray();
    for (EditorNode node : tree.nodes().values()) {
      layout.add(exportNode(node));
    }
    obj.add("layout", layout);

    return obj;
  }

  private JsonObject exportNode(EditorNode node) {
    JsonObject obj = new JsonObject();

    obj.addProperty("id", node.id());
    obj.addProperty("type", "ability");

    // Coordinates
    JsonObject coords = new JsonObject();
    Position pos = node.position();
    if (pos != null) {
      coords.addProperty("x", pos.x());
      coords.addProperty("y", pos.y());
    } else {
      coords.addProperty("x", 0);
      coords.addProperty("y", 0);
    }
    obj.add("coordinates", coords);

    // Archetype reference
    if (node.archetypeRef() != null) {
      obj.addProperty("archetype_ref", node.archetypeRef());
    }

    // Meta
    JsonObject meta = new JsonObject();
    meta.addProperty("name", node.name());

    // Description as array
    JsonArray descArray = new JsonArray();
    if (node.description() != null && !node.description().isEmpty()) {
      descArray.add(node.description());
    }
    meta.add("description", descArray);

    // Icon
    JsonObject icon = new JsonObject();
    icon.addProperty("id", node.icon().name());
    if (node.itemModel() != null) {
      icon.addProperty("item_model", node.itemModel());
    }
    meta.add("icon", icon);

    meta.addProperty("cost", node.cost());

    // Prerequisites
    JsonArray prereqs = new JsonArray();
    node.prerequisites().forEach(prereqs::add);
    meta.add("prerequisites", prereqs);

    // Perk ID and level
    if (node.perkId() != null && !node.perkId().isEmpty()) {
      meta.addProperty("perk_id", node.perkId());
      meta.addProperty("level", node.level());
    }

    // Effects
    JsonArray effects = new JsonArray();
    for (EditorEffect effect : node.effects()) {
      effects.add(exportEffect(effect));
    }
    meta.add("effects", effects);

    obj.add("meta", meta);

    return obj;
  }

  private JsonObject exportEffect(EditorEffect effect) {
    JsonObject obj = new JsonObject();

    switch (effect.type()) {
      case BOOST -> {
        obj.addProperty("type", "boost");
        obj.addProperty("target", effect.target());
        obj.addProperty("amount", effect.amount());
      }
      case PASSIVE -> {
        obj.addProperty("type", "passive");
        if (effect.ability() != null) {
          obj.addProperty("ability", effect.ability());
        }
        if (effect.passiveDescription() != null) {
          obj.addProperty("description", effect.passiveDescription());
        }
      }
      case PERMISSION -> {
        obj.addProperty("type", "permission");
        // Export permissions array if there are multiple, otherwise export single permission
        if (effect.permissions() != null && effect.permissions().size() > 1) {
          JsonArray permsArray = new JsonArray();
          for (String perm : effect.permissions()) {
            permsArray.add(perm);
          }
          obj.add("permissions", permsArray);
        } else if (effect.permission() != null) {
          obj.addProperty("permission", effect.permission());
        }
      }
      case RULED_BOOST -> {
        obj.addProperty("type", "ruled_boost");
        obj.addProperty("target", effect.target());
        if (effect.ruledDescription() != null) {
          obj.addProperty("description", effect.ruledDescription());
        }
        // TODO: Parse and export full ruled boost config
        obj.addProperty("_note", "Complex ruled boost - edit JSON manually");
      }
    }

    return obj;
  }
}
