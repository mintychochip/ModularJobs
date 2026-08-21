package dev.mintychochip;

import java.math.BigDecimal;
import dev.mintychochip.container.BoostSource;
import org.jetbrains.annotations.Contract;

/**
 * A pure function mapping payout inputs to a reward amount.
 *
 * <p>Implementations must be deterministic and side-effect free.</p>
 */
public interface PayableCurve {

  /**
   * Evaluates the curve for the given parameters.
   *
   * @param parameters the curve inputs
   * @return the curve value for {@code parameters}
   */
  @Contract(pure = true)
  BigDecimal evaluate(Parameters parameters);

  /** Inputs to {@link PayableCurve#evaluate(Parameters)}. */
  record Parameters(BigDecimal base, int level, int jobs) {

  }
}
