package net.aincraft.boost;

import net.aincraft.container.Codec;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class IntArrayCodecImpl implements Codec.Typed<int[]>{

  @Override
  public void encode(Out out, int[] object, Writer writer) {
    out.writeInt(object.length);
    for (int i : object) {
      out.writeInt(i);
    }
  }

  @Override
  public int[] decode(In in, Reader reader) {
    int len = in.readInt();
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
