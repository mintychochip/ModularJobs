package net.aincraft.container.boost;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;

public interface BoostData {
  BoostSource getBoostSource();
  interface SerializableBoostData extends BoostData {
    RuledBoostSource getBoostSource();

    interface ConsumableBoostData extends Timed {
      Optional<Duration> getDuration();
    }

    interface PassiveBoostData {
      Optional<SlotSet> getApplicableSlots();
    }
  }

  interface Timed {
    Optional<Duration> getDuration();
  }
}
