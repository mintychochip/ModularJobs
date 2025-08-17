package net.aincraft.serialization;

import java.time.Duration;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.SerializableBoostDataImpl;
import net.aincraft.container.SlotSetImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record SerializableBoostDataCodecImpl() implements Codec.Typed<SerializableBoostDataImpl> {

  @Override
  public void encode(BinaryOut out, SerializableBoostDataImpl object, Writer writer) {
    writer.write(out, object.boostSource());
    SlotSetImpl slotSet = object.slotSet();
    out.writeBool(slotSet != null);
    if (slotSet != null) {
      writer.write(out, slotSet);
    }
    Duration duration = object.duration();
    out.writeBool(duration != null);
    if (duration != null) {
      writer.write(out, duration);
    }
  }

  @Override
  public SerializableBoostDataImpl decode(BinaryIn in, Reader reader) {
    RuledBoostSourceImpl boostSource = reader.read(in, RuledBoostSourceImpl.class);
    SlotSetImpl slotSet = in.readBool() ? reader.read(in, SlotSetImpl.class) : null;
    Duration duration = in.readBool() ? reader.read(in, Duration.class) : null;
    return new SerializableBoostDataImpl(boostSource, slotSet, duration);
  }

  @Override
  public Class<?> type() {
    return SerializableBoostDataImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:serializable_boost_data");
  }
}
