package net.aincraft.upgrade.tree;

import java.util.List;
import java.util.Optional;
import net.aincraft.upgrade.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single item in a skill tree layout.
 * Can be either an ability node or a connector node.
 *
 * @param id            unique identifier for this item
 * @param type          whether this is an ability or connector
 * @param coordinates   position in the GUI grid
 * @param meta          metadata (either AbilityMeta or ConnectorMeta)
 * @param archetypeRef  optional reference to an archetype ID
 * @param family        optional list of related node IDs (for grouping)
 */
public record LayoutItem(
    @NotNull String id,
    @NotNull LayoutItemType type,
    @NotNull Position coordinates,
    @NotNull LayoutItemMeta meta,
    @Nullable String archetypeRef,
    @Nullable List<String> family
) {
  /**
   * Get the ability metadata if this is an ability node.
   */
  public Optional<AbilityMeta> abilityMeta() {
    return meta instanceof AbilityMeta am ? Optional.of(am) : Optional.empty();
  }

  /**
   * Get the connector metadata if this is a connector node.
   *
   * @deprecated Connector nodes are deprecated. Use {@code path_from_parent} on ability nodes instead.
   */
  @Deprecated(since = "1.0", forRemoval = true)
  public Optional<ConnectorMeta> connectorMeta() {
    return meta instanceof ConnectorMeta cm ? Optional.of(cm) : Optional.empty();
  }
}
