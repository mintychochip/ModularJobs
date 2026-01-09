package net.aincraft.upgrade.wynncraft;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for connector nodes in a Wynncraft-style skill tree.
 * Connector nodes are visual path segments that connect ability nodes.
 * They have locked and unlocked states with different icons.
 *
 * @param links  list of node IDs that this connector connects (typically 2: start and end)
 * @param shapes icon configurations for locked and unlocked states
 */
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
