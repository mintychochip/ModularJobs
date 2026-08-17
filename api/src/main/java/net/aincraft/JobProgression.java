package net.aincraft;

import java.math.BigDecimal;

/**
 * The mutable progression of a single player in a single job.
 *
 * <p>Each operation returns a new {@code JobProgression} instance rather than mutating the
 * receiver, so instances are effectively immutable snapshots of progression state.</p>
 */
public interface JobProgression extends JobProgressionView {

  /**
   * Replaces the player's accumulated experience, returning the resulting progression.
   *
   * @param experience the new total experience
   * @return a new progression carrying the given experience, or {@code this} unchanged when the
   *         value is already set
   */
  JobProgression setExperience(BigDecimal experience);

  /**
   * Adds experience to the current total.
   *
   * @param experience the amount to add
   * @return the progression with {@code experience} added to {@link #experience()}
   */
  default JobProgression addExperience(BigDecimal experience) {
    return setExperience(experience.add(experience()));
  }
}
