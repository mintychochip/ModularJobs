package dev.mintychochip.container.boost.factories;

import java.math.BigDecimal;
import dev.mintychochip.container.Boost;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Factory for constructing {@link Boost} instances.
 * <p>
 * Internal API: obtain an instance through {@link Boost#factory()} rather than
 * implementing or instantiating this interface directly.
 */
@Internal
public interface BoostFactory {

  /**
   * Creates a boost that adds the given amount.
   *
   * @param amount amount added by the boost
   * @return an additive boost
   */
  Boost additive(BigDecimal amount);

  /**
   * Creates a boost that multiplies by the given amount.
   *
   * @param amount factor the boost multiplies by
   * @return a multiplicative boost
   */
  Boost multiplicative(BigDecimal amount);
}
