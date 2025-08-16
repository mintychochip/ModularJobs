package net.aincraft.container;

import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Codec extends Keyed {

  Class<?> type();

  non-sealed interface Typed<T> extends Codec {

    void encode(Out out, T object, Writer writer);

    T decode(In in, Reader reader);
  }

  interface Writer {

    void write(Out out, Object child);
  }

  interface Reader {

    Object read(In in);

    default <T> T read(In in, Class<T> clazz) throws ClassCastException {
      Object object = read(in);
      return clazz.cast(object);
    }
  }
}
