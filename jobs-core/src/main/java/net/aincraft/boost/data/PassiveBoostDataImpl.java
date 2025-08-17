package net.aincraft.boost.data;

import net.aincraft.container.boost.ItemBoostData.PassiveBoostData;
import net.aincraft.container.boost.RuledBoostSource;

public record PassiveBoostDataImpl(RuledBoostSource boostSource, int[] slots) implements
    PassiveBoostData {

  @Override
  public int[] getApplicableSlots() {
    return slots;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

}
