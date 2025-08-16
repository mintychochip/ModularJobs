package net.aincraft.serialization.data;

import java.time.Duration;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.ItemBoostData;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record CombinedBoostDataImpl(RuledBoostSource boostSource, Duration duration,
                             int[] slots) implements ItemBoostData.Passive,
    ItemBoostData.Consumable {

  @Override
  public Duration getDuration() {
    return duration;
  }

  @Override
  public int[] getApplicableSlots() {
    return slots;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

  static final class CodecImpl implements Codec.Typed<CombinedBoostDataImpl> {

    @Override
    public void encode(Out out, CombinedBoostDataImpl object, Writer writer) {

    }

    @Override
    public CombinedBoostDataImpl decode(In in, Reader reader) {
      return null;
    }

    @Override
    public Class<?> type() {
      return null;
    }

    @Override
    public @NotNull Key key() {
      return null;
    }
  }
}
