package net.aincraft.api.container;

import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Codec extends Keyed {

  Class<?> type();

  non-sealed interface Typed<T> extends Codec {

    void encode(Out out, T object, Writer writer);

    Object decode(In in, Reader reader);
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
