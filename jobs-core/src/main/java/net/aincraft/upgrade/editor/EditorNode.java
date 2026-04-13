package net.aincraft.upgrade.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.UpgradeEffect;
import net.aincraft.upgrade.UpgradeNode;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable upgrade node for editing purposes.
 */
public final class EditorNode {

  private String id;
  private String name;
  private String description;
  private Material icon;
  private Material unlockedIcon;
  private String itemModel;
  private String unlockedItemModel;
  private int cost;
  private Position position;
  private String archetypeRef;
  private String perkId;
  private int level;

  private final Set<String> prerequisites = new HashSet<>();
  private final Set<String> prerequisitesOr = new HashSet<>();
  private final Set<String> exclusive = new HashSet<>();
  private final List<String> children = new ArrayList<>();
  private final List<EditorEffect> effects = new ArrayList<>();

  public EditorNode() {
    this.id = "";
    this.name = "New Node";
    this.description = "";
    this.icon = Material.STONE;
    this.unlockedIcon = Material.STONE;
    this.itemModel = null;
    this.unlockedItemModel = null;
    this.cost = 1;
    this.position = new Position(0, 0);
    this.archetypeRef = null;
    this.perkId = "";
    this.level = 1;
  }

  /**
   * Create from an existing UpgradeNode.
   */
  public static EditorNode fromUpgradeNode(@NotNull UpgradeNode source) {
    EditorNode node = new EditorNode();

    // Extract short key from namespaced key
    String fullKey = source.key().asString();
    int colonIndex = fullKey.indexOf(':');
    node.id = colonIndex >= 0 ? fullKey.substring(colonIndex + 1) : fullKey;

    node.name = source.name();
    node.description = source.description();
    node.icon = source.icon();
    node.unlockedIcon = source.unlockedIcon();
    node.itemModel = source.itemModel();
    node.unlockedItemModel = source.unlockedItemModel();
    node.cost = source.cost();
    node.position = source.position();
    node.perkId = source.perkId();
    node.level = source.level();

    node.prerequisites.addAll(source.prerequisites());
    node.prerequisitesOr.addAll(source.prerequisitesOr());
    node.exclusive.addAll(source.exclusive());
    node.children.addAll(source.children());

    // Convert effects
    for (UpgradeEffect effect : source.effects()) {
      node.effects.add(EditorEffect.fromUpgradeEffect(effect));
    }

    return node;
  }

  // ========== Getters/Setters ==========

  public String id() { return id; }
  public void setId(String id) { this.id = id; }

  public String name() { return name; }
  public void setName(String name) { this.name = name; }

  public String description() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Material icon() { return icon; }
  public void setIcon(Material icon) { this.icon = icon; }

  public Material unlockedIcon() { return unlockedIcon; }
  public void setUnlockedIcon(Material icon) { this.unlockedIcon = icon; }

  public String itemModel() { return itemModel; }
  public void setItemModel(String itemModel) { this.itemModel = itemModel; }

  public String unlockedItemModel() { return unlockedItemModel; }
  public void setUnlockedItemModel(String unlockedItemModel) { this.unlockedItemModel = unlockedItemModel; }

  public int cost() { return cost; }
  public void setCost(int cost) { this.cost = Math.max(0, cost); }

  public Position position() { return position; }
  public void setPosition(Position position) { this.position = position; }

  public String archetypeRef() { return archetypeRef; }
  public void setArchetypeRef(String archetypeRef) { this.archetypeRef = archetypeRef; }

  public String perkId() { return perkId; }
  public void setPerkId(String perkId) { this.perkId = perkId; }

  public int level() { return level; }
  public void setLevel(int level) { this.level = Math.max(1, level); }

  public Set<String> prerequisites() { return prerequisites; }
  public Set<String> prerequisitesOr() { return prerequisitesOr; }
  public Set<String> exclusive() { return exclusive; }
  public List<String> children() { return children; }
  public List<EditorEffect> effects() { return effects; }

  // ========== Helpers ==========

  /**
   * Check if this is a root node (no prerequisites).
   */
  public boolean isRoot() {
    return prerequisites.isEmpty();
  }

  /**
   * Create a deep copy of this node.
   */
  public EditorNode copy() {
    EditorNode copy = new EditorNode();
    copy.id = this.id;
    copy.name = this.name;
    copy.description = this.description;
    copy.icon = this.icon;
    copy.unlockedIcon = this.unlockedIcon;
    copy.itemModel = this.itemModel;
    copy.unlockedItemModel = this.unlockedItemModel;
    copy.cost = this.cost;
    copy.position = this.position; // Position is immutable
    copy.archetypeRef = this.archetypeRef;
    copy.perkId = this.perkId;
    copy.level = this.level;
    copy.prerequisites.addAll(this.prerequisites);
    copy.prerequisitesOr.addAll(this.prerequisitesOr);
    copy.exclusive.addAll(this.exclusive);
    copy.children.addAll(this.children);
    copy.effects.addAll(this.effects.stream().map(EditorEffect::copy).toList());
    return copy;
  }
}
