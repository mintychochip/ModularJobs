package net.aincraft.upgrade;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single ability node in a job's upgrade tree.
 * Connectors are purely a client rendering concern (see {@link ConnectorNode}).
 *
 * @param key              unique identifier (e.g., "miner:efficiency_1")
 * @param name             display name shown in UI
 * @param description      description of what this upgrade does
 * @param icon             material to display when locked
 * @param unlockedIcon     material to display when unlocked
 * @param itemModel        item model namespace:key when locked (null = none)
 * @param unlockedItemModel item model namespace:key when unlocked (null = none)
 * @param cost             skill point cost to unlock
 * @param prerequisites    node keys that must ALL be unlocked first (AND logic)
 * @param prerequisitesOr  node keys where at least ONE must be unlocked (OR logic)
 * @param exclusive        node keys that become locked if this is chosen
 * @param children         node keys that this node leads to
 * @param effects          list of effects granted by this upgrade
 * @param position         optional position for UI rendering (x, y)
 * @param pathPoints       explicit path points from this node back to its parent (empty for root)
 * @param perkId           perk identifier (e.g., "crit_chance", "fortune_spec")
 * @param level            perk level (1, 2, 3...)
 * @param maxLevel         max upgradeable level (0 or 1 = not upgradeable, >1 = upgradeable in place)
 * @param levelCosts       cost per level upgrade [lvl1, lvl2, lvl3], empty = use cost for all
 * @param levelDescriptions description per level [lvl1, lvl2, lvl3], empty = use base description
 * @param levelEffects     effects per level [[lvl1 effects], [lvl2 effects], ...], empty = use base effects
 * @param levelIcons       icon material per level [lvl1, lvl2, lvl3], empty = use base icon
 * @param levelItemModels  item model per level [lvl1, lvl2, lvl3], empty = use base model
 * @param nodeTexture      texture name for custom node rendering (e.g., "blue", "warrior"), null = use material icons
 */
public record UpgradeNode(
    @NotNull Key key,
    @NotNull String name,
    @Nullable String description,
    @NotNull Material icon,
    @NotNull Material unlockedIcon,
    @Nullable String itemModel,
    @Nullable String unlockedItemModel,
    int cost,
    @NotNull Set<String> prerequisites,
    @NotNull Set<String> prerequisitesOr,
    @NotNull Set<String> exclusive,
    @NotNull List<String> children,
    @NotNull List<UpgradeEffect> effects,
    @Nullable Position position,
    @NotNull List<Position> pathPoints,
    @NotNull String perkId,
    int level,
    int maxLevel,
    @NotNull List<Integer> levelCosts,
    @NotNull List<String> levelDescriptions,
    @NotNull List<List<UpgradeEffect>> levelEffects,
    @NotNull List<Material> levelIcons,
    @NotNull List<String> levelItemModels,
    @Nullable String nodeTexture
) implements Keyed {

  /**
   * Check if this node supports in-place upgrades (maxLevel > 1).
   */
  public boolean isUpgradeable() {
    return maxLevel > 1;
  }

  /**
   * Get the cost to upgrade to a specific level.
   *
   * @param targetLevel the level to upgrade to (1 = initial unlock)
   * @return SP cost for that level
   */
  public int getCostForLevel(int targetLevel) {
    if (levelCosts.isEmpty()) {
      return cost; // Fallback to base cost
    }
    int index = targetLevel - 1; // Level 1 = index 0
    if (index < 0 || index >= levelCosts.size()) {
      return cost; // Fallback
    }
    return levelCosts.get(index);
  }

  /**
   * Get description for a specific level (1-indexed).
   * Falls back to base description if not available.
   *
   * @param targetLevel the level (1, 2, 3...)
   * @return description for that level
   */
  @Nullable
  public String getDescriptionForLevel(int targetLevel) {
    if (levelDescriptions.isEmpty()) {
      return description;
    }
    int index = targetLevel - 1;
    if (index < 0 || index >= levelDescriptions.size()) {
      return description;
    }
    return levelDescriptions.get(index);
  }

  /**
   * Get effects for a specific level (1-indexed).
   * Falls back to base effects if not available.
   *
   * @param targetLevel the level (1, 2, 3...)
   * @return effects for that level
   */
  @NotNull
  public List<UpgradeEffect> getEffectsForLevel(int targetLevel) {
    if (levelEffects.isEmpty()) {
      return effects;
    }
    int index = targetLevel - 1;
    if (index < 0 || index >= levelEffects.size()) {
      return effects;
    }
    return levelEffects.get(index);
  }

  /**
   * Get icon material for a specific level (1-indexed).
   * Falls back to unlockedIcon if not available.
   *
   * @param targetLevel the level (1, 2, 3...)
   * @return icon material for that level
   */
  @NotNull
  public Material getIconForLevel(int targetLevel) {
    if (levelIcons.isEmpty()) {
      return unlockedIcon;
    }
    int index = targetLevel - 1;
    if (index < 0 || index >= levelIcons.size()) {
      return unlockedIcon;
    }
    return levelIcons.get(index);
  }

  /**
   * Get item model for a specific level (1-indexed).
   * Falls back to unlockedItemModel if not available.
   *
   * @param targetLevel the level (1, 2, 3...)
   * @return item model namespace:key for that level (null if none)
   */
  @Nullable
  public String getItemModelForLevel(int targetLevel) {
    if (levelItemModels.isEmpty()) {
      return unlockedItemModel;
    }
    int index = targetLevel - 1;
    if (index < 0 || index >= levelItemModels.size()) {
      return unlockedItemModel;
    }
    String model = levelItemModels.get(index);
    return model != null && !model.isEmpty() ? model : unlockedItemModel;
  }

  /**
   * Check if this node is a root node (no prerequisites).
   */
  public boolean isRoot() {
    return prerequisites.isEmpty() && prerequisitesOr.isEmpty();
  }

  /**
   * Check if this node is an ability (grants effects).
   * All UpgradeNode instances are abilities - use this for generic filtering.
   */
  public boolean isAbility() {
    return true;
  }

  /**
   * Get the appropriate icon material based on unlock state.
   *
   * @param unlocked true if the node is unlocked
   * @return the locked or unlocked icon
   */
  public Material getIconForState(boolean unlocked) {
    return unlocked ? unlockedIcon : icon;
  }

  /**
   * Get the appropriate item model based on unlock state.
   *
   * @param unlocked true if the node is unlocked
   * @return the item model namespace:key (null if none)
   */
  @Nullable
  public String getItemModelForState(boolean unlocked) {
    return unlocked ? unlockedItemModel : itemModel;
  }
}
