package net.aincraft.upgrade.wynncraft;

/**
 * Types of layout items in a Wynncraft-style skill tree.
 */
public enum LayoutItemType {
  /**
   * Ability nodes are playable upgrades that grant effects.
   */
  ABILITY,

  /**
   * Connector nodes are visual path segments between abilities.
   *
   * @deprecated Connector nodes are deprecated. Use {@code path_from_parent} on ability nodes instead.
   */
  @Deprecated(since = "1.0", forRemoval = true)
  CONNECTOR
}
