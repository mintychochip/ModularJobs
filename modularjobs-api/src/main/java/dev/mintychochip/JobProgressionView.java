package dev.mintychochip;

import java.math.BigDecimal;
import java.util.UUID;

/** A read-only view of a player's progression in a single job. */
public interface JobProgressionView {

  /**
   * Returns the total experience required to reach the given level.
   *
   * @param level the target level
   * @return the experience threshold for {@code level}
   */
  BigDecimal experienceForLevel(int level);

  /** Job. */
  Job job();

  /** Player id. */
  UUID playerId();

  /** Experience. */
  BigDecimal experience();

  /** Level. */
  int level();
}
