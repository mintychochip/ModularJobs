package net.aincraft.upgrade.wynncraft;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for connector nodes in a Wynncraft-style skill tree.
 * Connector nodes are visual path segments that connect ability nodes.
 * They have locked and unlocked states with different icons.
 *
 * @deprecated Connector nodes are deprecated in favor of {@code path_from_parent} on ability nodes.
 *             Use the {@code path_from_parent} field in {@code AbilityMeta} to define visual paths.
 *             This class will be removed in a future version.
 *
 * @param links  list of node IDs that this connector connects (typically 2: start and end)
 * @param shapes icon configurations for locked and unlocked states
 */
@Deprecated(since = "1.0", forRemoval = true)
public record ConnectorMeta(
    @NotNull List<String> links,
    @NotNull ConnectorShapes shapes
) implements LayoutItemMeta {
  /**
   * Icon configurations for connector states.
   *
   * @param locked   icon shown when path is locked (at least one endpoint not unlocked)
   * @param unlocked icon shown when path is unlocked (both endpoints unlocked)
   */
  public record ConnectorShapes(
      @NotNull IconConfig locked,
      @NotNull IconConfig unlocked
  ) {
  }
}
