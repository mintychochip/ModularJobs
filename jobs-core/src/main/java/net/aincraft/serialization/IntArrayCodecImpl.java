package net.aincraft.serialization;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record IntArrayCodecImpl() implements Codec.Typed<int[]>{

  @Override
  public void encode(BinaryOut out, int[] object, Writer writer) {
    out.writeUnsignedInt(object.length);
    for (int i : object) {
      out.writeInt(i);
    }
  }

  @Override
  public int[] decode(BinaryIn in, Reader reader) {
    int len = in.readUnsignedInt();
    int[] array = new int[len];
    for (int i = 0; i < len; ++i) {
      array[i] = in.readInt();
    }
    return array;
  }

  @Override
  public Class<?> type() {
    return int[].class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:int_array");
  }
}
