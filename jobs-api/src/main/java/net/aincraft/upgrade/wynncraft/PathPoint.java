package net.aincraft.upgrade.wynncraft;

import net.aincraft.upgrade.Position;
import org.jetbrains.annotations.NotNull;

/**
 * A path point with optional connector type for visual rendering.
 * Path points represent intermediate points in skill tree connections.
 *
 * @param position the grid position (x, y coordinates)
 * @param type     the connector type (line, dashed, curve, etc.)
 */
public record PathPoint(
    @NotNull Position position,
    @NotNull PathType type
) {
  /**
   * Create a path point with default LINE type.
   */
  public PathPoint(@NotNull Position position) {
    this(position, PathType.DEFAULT);
  }

  /**
   * Create a path point from x, y coordinates with default LINE type.
   */
  public PathPoint(int x, int y) {
    this(new Position(x, y), PathType.DEFAULT);
  }

  /**
   * Create a path point from x, y coordinates with specified type.
   */
  public PathPoint(int x, int y, @NotNull PathType type) {
    this(new Position(x, y), type);
  }

  /** X coordinate of the path point. */
  public int x() {
    return position.x();
  }

  /** Y coordinate of the path point. */
  public int y() {
    return position.y();
  }
}
