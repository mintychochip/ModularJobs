package dev.mintychochip.container.boost;

import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import java.time.Duration;
import java.util.BitSet;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Data describing the source and persistence-related state of a boost. */
public sealed interface BoostData permits SerializableBoostData {

  /**
   * Returns the source that produced this boost data.
   *
   * @return the boost source
   */
  @NotNull
  BoostSource boostSource();

  /** Boost data whose state can be serialized. */
  sealed interface SerializableBoostData extends BoostData
      permits PassiveBoostData, ConsumableBoostData {

    /**
     * Serializable data for a boost that can be consumed for a duration.
     *
     * @param boostSource source of the boost
     * @param duration duration for which the boost is active
     */
    record ConsumableBoostData(@NotNull BoostSource boostSource, @NotNull Duration duration)
        implements TimedBoostData, SerializableBoostData {

      /**
       * Returns this boost's configured duration.
       *
       * @return the configured duration
       */
      @Override
      public Optional<Duration> getDuration() {
        return Optional.of(duration);
      }
    }

    /**
     * Serializable data for a boost that remains passive and tracks its slots.
     *
     * @param boostSource source of the boost
     * @param slotSet slots associated with the passive boost
     */
    record PassiveBoostData(@NotNull BoostSource boostSource, @NotNull BitSet slotSet)
        implements SerializableBoostData {}
  }

  /** Data that exposes an optional active duration. */
  @FunctionalInterface
  interface TimedBoostData {

    /**
     * Returns the duration, when this boost data is timed.
     *
     * @return the boost duration
     */
    Optional<Duration> getDuration();
  }
}
