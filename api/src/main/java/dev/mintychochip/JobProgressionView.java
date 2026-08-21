package dev.mintychochip;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A read-only view of a player's progression in a single job.
 */
public interface JobProgressionView {

  /**
   * Returns the total experience required to reach the given level.
   *
   * @param level the target level
   * @return the experience threshold for {@code level}
   */
  BigDecimal experienceForLevel(int level);

  /** Returns the job this progression applies to. */
  Job job();

  /** Returns the owning player. */
  UUID playerId();

  /** Returns the player's accumulated experience. */
  BigDecimal experience();

  /** Returns the player's current level, derived from {@link #experience()}. */
  int level();
}
