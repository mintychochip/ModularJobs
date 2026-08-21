package dev.mintychochip;

import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;

/**
 * A pure function mapping leveling inputs to an experience value.
 *
 * <p>Implementations must be deterministic and side-effect free.</p>
 */
public interface LevelingCurve {

  /**
   * Evaluates the curve for the given parameters.
   *
   * @param parameters the curve inputs
   * @return the curve value for {@code parameters}
   */
  @Contract(pure = true)
  BigDecimal evaluate(Parameters parameters);

  /** Inputs to {@link LevelingCurve#evaluate(Parameters)}. */
  record Parameters(int level) {

  }
}
