package net.aincraft.upgrade.wynncraft;

/**
 * Types of connector/visual path segments in a skill tree.
 * Determines how the path is rendered in the GUI.
 */
public enum PathType {
  /**
   * Standard solid line connection between nodes.
   */
  LINE,

  /**
   * Dashed or dotted line, typically for optional paths.
   */
  DASHED,

  /**
   * Curved path connector.
   */
  CURVE,

  /**
   * Corner/turn point in a path (where direction changes).
   */
  CORNER,

  /**
   * Default/unknown type - renders as standard line.
   */
  DEFAULT
}
