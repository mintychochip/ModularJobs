package net.aincraft.serialization;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Codec extends Keyed {

  Class<?> type();

  non-sealed interface Typed<T> extends Codec {

    void encode(BinaryOut out, T object, Writer writer);

    T decode(BinaryIn in, Reader reader);
  }

  interface Writer {

    void write(BinaryOut out, Object child);
  }

  interface Reader {

    Object read(BinaryIn in);

    default <T> T read(BinaryIn in, Class<T> clazz) throws ClassCastException {
      Object object = read(in);
      return clazz.cast(object);
    }
  }
}
