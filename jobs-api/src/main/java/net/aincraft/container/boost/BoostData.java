package net.aincraft.container.boost;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import org.jetbrains.annotations.NotNull;

public sealed interface BoostData permits SerializableBoostData {

  @NotNull
  BoostSource boostSource();

  sealed interface SerializableBoostData extends BoostData permits PassiveBoostData,
      ConsumableBoostData {

    record ConsumableBoostData(@NotNull BoostSource boostSource,
                               @NotNull Duration duration) implements TimedBoostData,
        SerializableBoostData {

      @Override
      public Optional<Duration> getDuration() {
        return Optional.of(duration);
      }
    }

    record PassiveBoostData(@NotNull BoostSource boostSource, @NotNull SlotSet slotSet) implements
        SerializableBoostData {

    }
  }

  interface TimedBoostData {

    Optional<Duration> getDuration();
  }

}
