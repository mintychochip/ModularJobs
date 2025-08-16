package net.aincraft.container.boost;

import java.time.Duration;
import net.aincraft.container.RuledBoostSource;
import net.aincraft.container.boost.ItemBoostData.Consumable;
import net.aincraft.container.boost.ItemBoostData.Passive;

public sealed interface ItemBoostData permits Passive, Consumable {

  RuledBoostSource getBoostSource();

  static ItemBoostData consumable() {
    return new ConsumableBoostDataImpl();
  }

  static ItemBoostData passive() {
    return new PassiveBoostDataImpl();
  }

  static ItemBoostData combined() {

  }

  non-sealed interface Consumable extends ItemBoostData {

    Duration getDuration();
  }

  non-sealed interface Passive extends ItemBoostData {

    int[] getApplicableSlots();
  }

  interface Builder {

    Builder withDuration(Duration duration);

    Builder withSlots(int[] slots);

    Builder withBoostSource(RuledBoostSource boostSource);

    ItemBoostData build() throws IllegalStateException;
  }
}
