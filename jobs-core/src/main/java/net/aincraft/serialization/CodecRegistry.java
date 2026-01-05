package net.aincraft.serialization;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public interface CodecRegistry {

  byte @NotNull [] encode(Object object);

  Object decode(byte[] bytes);

  <T> T decode(byte[] bytes, Class<T> clazz);
}
