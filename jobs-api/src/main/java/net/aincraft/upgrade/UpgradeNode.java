package net.aincraft.upgrade;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single node in a job's upgrade tree.
 *
 * @param key           unique identifier (e.g., "miner:efficiency_1")
 * @param name          display name shown in UI
 * @param description   description of what this upgrade does
 * @param icon          material to display as icon
 * @param cost          skill point cost to unlock
 * @param nodeType      whether this is a minor or major (specialization) upgrade
 * @param prerequisites node keys that must be unlocked first
 * @param exclusive     node keys that become locked if this is chosen (for specializations)
 * @param children      node keys that this node leads to
 * @param effects       list of effects granted by this upgrade
 * @param position      optional position for UI rendering (x, y)
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
    @Nullable Position position
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
   * Position for UI rendering.
   */
  public record Position(int x, int y) {
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
}
