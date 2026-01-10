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
 * @param prerequisites    node keys that must be unlocked first
 * @param exclusive        node keys that become locked if this is chosen
 * @param children         node keys that this node leads to
 * @param effects          list of effects granted by this upgrade
 * @param position         optional position for UI rendering (x, y)
 * @param pathPoints       explicit path points from this node back to its parent (empty for root)
 * @param perkId           perk identifier (e.g., "crit_chance", "fortune_spec")
 * @param level            perk level (1, 2, 3...)
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
    @NotNull Set<String> exclusive,
    @NotNull List<String> children,
    @NotNull List<UpgradeEffect> effects,
    @Nullable Position position,
    @NotNull List<Position> pathPoints,
    @NotNull String perkId,
    int level
) implements Keyed {

  /**
   * Check if this node is a root node (no prerequisites).
   */
  public boolean isRoot() {
    return prerequisites.isEmpty();
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
