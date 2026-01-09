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
 * Only contains MINOR (regular) and MAJOR (specialization) ability nodes.
 * Connectors are purely a client rendering concern (see {@link ConnectorNode}).
 *
 * @param key           unique identifier (e.g., "miner:efficiency_1")
 * @param name          display name shown in UI
 * @param description   description of what this upgrade does
 * @param icon          material to display as icon
 * @param cost          skill point cost to unlock
 * @param nodeType      whether this is MINOR or MAJOR
 * @param prerequisites node keys that must be unlocked first
 * @param exclusive     node keys that become locked if this is chosen
 * @param children      node keys that this node leads to
 * @param effects       list of effects granted by this upgrade
 * @param position      optional position for UI rendering (x, y)
 * @param pathPoints    explicit path points from this node back to its parent (empty for root)
 * @param perkId        perk identifier (e.g., "crit_chance", "fortune_spec")
 * @param level         perk level (1, 2, 3...; majors are always 1)
 */
public record UpgradeNode(
    @NotNull Key key,
    @NotNull String name,
    @Nullable String description,
    @NotNull Material icon,
    int cost,
    @NotNull NodeType nodeType,
    @NotNull Set<String> prerequisites,
    @NotNull Set<String> exclusive,
    @NotNull List<String> children,
    @NotNull List<UpgradeEffect> effects,
    @Nullable Position position,
    @NotNull List<Position> pathPoints,
    @NotNull String perkId,
    int level
) implements Keyed {

  public enum NodeType {
    /**
     * Regular upgrade node.
     */
    MINOR,
    /**
     * Major upgrade that typically branches the tree (specialization).
     */
    MAJOR
  }

  /**
   * Check if this node is a root node (no prerequisites).
   */
  public boolean isRoot() {
    return prerequisites.isEmpty();
  }

  /**
   * Check if this is a specialization node.
   */
  public boolean isSpecialization() {
    return nodeType == NodeType.MAJOR;
  }

  /**
   * Check if this node is an ability (grants effects).
   * All UpgradeNode instances are abilities - use this for generic filtering.
   */
  public boolean isAbility() {
    return true;
  }
}
