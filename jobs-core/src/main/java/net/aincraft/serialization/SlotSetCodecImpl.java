package net.aincraft.serialization;

import net.aincraft.container.SlotSetImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record SlotSetCodecImpl() implements Codec.Typed<SlotSetImpl> {

  @Override
  public void encode(BinaryOut out, SlotSetImpl object, Writer writer) {
    out.writeLong(object.asLong());
  }

  @Override
  public SlotSetImpl decode(BinaryIn in, Reader reader) {
    return new SlotSetImpl(in.readLong());
  }

  @Override
  public Class<?> type() {
    return SlotSetImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:slot_set");
  }
}
