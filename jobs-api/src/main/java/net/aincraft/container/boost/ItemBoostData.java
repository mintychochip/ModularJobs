package net.aincraft.container.boost;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.Bridge;
import net.aincraft.container.boost.ItemBoostData.ConsumableBoostData;
import net.aincraft.container.boost.ItemBoostData.PassiveBoostData;
import net.aincraft.container.boost.factories.ItemBoostDataFactory;

public sealed interface ItemBoostData permits PassiveBoostData, ConsumableBoostData {

  ItemBoostDataFactory FACTORY = Bridge.bridge().itemBoostDataFactory();

  RuledBoostSource getBoostSource();

  non-sealed interface ConsumableBoostData extends ItemBoostData, TimedBoostData {

    Optional<Duration> getDuration();
  }

  non-sealed interface PassiveBoostData extends ItemBoostData {

    int[] getApplicableSlots();
  }

  interface Builder {

    Builder withDuration(Duration duration);

    Builder withSlots(int[] slots);

    Builder withBoostSource(RuledBoostSource boostSource);

    ItemBoostData build() throws IllegalStateException;
  }
}
