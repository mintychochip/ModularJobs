package net.aincraft.upgrade;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a connector node in a skill tree.
 * Connector nodes are visual path segments that connect ability nodes.
 * They have different visual states for locked and unlocked paths.
 *
 * @deprecated Connector nodes are deprecated in favor of {@code path_from_parent} on ability nodes.
 *             Use the {@code path_from_parent} field in {@code AbilityMeta} to define visual paths.
 *             This class will be removed in a future version.
 *
 * @param key            unique identifier (e.g., "miner:connector_basics_to_eff1")
 * @param name           display name (typically "Connector" or similar)
 * @param icon           locked state icon
 * @param unlockedIcon   unlocked state icon
 * @param links          node IDs that this connector connects (start and end)
 * @param position       position for UI rendering (x, y)
 * @param lockedCustomModelData   custom model data for locked state (0 if unused)
 * @param unlockedCustomModelData custom model data for unlocked state (0 if unused)
 */
@Deprecated(since = "1.0", forRemoval = true)
public record ConnectorNode(
    @NotNull Key key,
    @NotNull String name,
    @NotNull Material icon,
    @NotNull Material unlockedIcon,
    @NotNull List<String> links,
    @NotNull Position position,
    int lockedCustomModelData,
    int unlockedCustomModelData
) implements Keyed {

  /**
   * Check if this connector connects two specific nodes.
   *
   * @param nodeKey1 first node ID
   * @param nodeKey2 second node ID
   * @return true if this connector links both nodes (order-independent)
   */
  public boolean connects(String nodeKey1, String nodeKey2) {
    return links.contains(nodeKey1) && links.contains(nodeKey2);
  }

  /**
   * Get the icon to display based on unlock status.
   *
   * @param unlocked true if path is unlocked
   * @return the appropriate icon material
   */
  public Material getIconForState(boolean unlocked) {
    return unlocked ? unlockedIcon : icon;
  }

  /**
   * Check if connector is unlocked given a set of unlocked node keys.
   * A connector is unlocked when BOTH linked nodes are unlocked.
   *
   * @param unlockedNodes set of currently unlocked node keys
   * @return true if connector should show as unlocked
   */
  public boolean isUnlocked(Set<String> unlockedNodes) {
    return unlockedNodes.containsAll(links);
  }
}
