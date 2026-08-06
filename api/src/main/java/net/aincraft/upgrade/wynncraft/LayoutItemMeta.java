package net.aincraft.upgrade.wynncraft;

/**
 * Sealed interface for layout item metadata.
 * Implementation is either AbilityMeta or ConnectorMeta.
 */
public sealed interface LayoutItemMeta permits AbilityMeta, ConnectorMeta {
}
