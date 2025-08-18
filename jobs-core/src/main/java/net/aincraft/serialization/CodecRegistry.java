package net.aincraft.serialization;

import net.aincraft.registry.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public interface CodecRegistry extends Registry<Codec> {

  byte @NotNull [] encode(Object object);

  Object decode(byte[] bytes);

  <T> T decode(byte[] bytes, Class<T> clazz);
}
