package net.aincraft.serialization.data;

import com.google.common.base.Preconditions;
import java.time.Duration;
import net.aincraft.container.RuledBoostSource;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.ItemBoostData.Builder;
import net.aincraft.container.boost.factories.ItemBoostDataFactory;

public final class ItemBoostDataFactoryImpl implements ItemBoostDataFactory {

  @Override
  public Builder builder() {
    return null;
  }

  static final class BuilderImpl implements Builder {

    private RuledBoostSource boostSource = null;
    private Duration duration = null;
    private int[] slots = null;

    @Override
    public Builder withDuration(Duration duration) {
      this.duration = duration;
      return this;
    }

    @Override
    public Builder withSlots(int[] slots) {
      this.slots = slots;
      return this;
    }

    @Override
    public Builder withBoostSource(RuledBoostSource boostSource) {
      this.boostSource = boostSource;
      return this;
    }

    @Override
    public ItemBoostData build() throws IllegalStateException {
      Preconditions.checkState(boostSource != null);
      if (duration != null && slots != null) {
        return new CombinedBoostDataImpl(boostSource, duration, slots);
      }
      if (duration != null) {
        return new ConsumableBoostDataImpl(boostSource, duration);
      }
      if (slots != null) {
        return new PassiveBoostDataImpl(boostSource, slots);
      }
      throw new IllegalStateException("durations and slots cannot be null");
    }
  }
}
