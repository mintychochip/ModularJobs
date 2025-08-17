package net.aincraft.boost;

import static net.aincraft.container.boost.BoostData.SerializableBoostData.*;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.container.SlotSet;
import net.aincraft.container.SlotSetImpl;
import net.aincraft.container.boost.RuledBoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import org.jetbrains.annotations.Nullable;

public record SerializableBoostDataImpl(RuledBoostSourceImpl boostSource, @Nullable SlotSetImpl slotSet,
                                        @Nullable Duration duration) implements SerializableBoostData,
    ConsumableBoostData, PassiveBoostData {

  @Override
  public Optional<SlotSet> getApplicableSlots() {
    return Optional.ofNullable(slotSet);
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

  @Override
  public Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }
}
