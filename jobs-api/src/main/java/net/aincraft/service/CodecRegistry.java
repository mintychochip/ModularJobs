package net.aincraft.service;

import net.aincraft.serialization.Codec;
import net.aincraft.registry.Registry;
import org.jetbrains.annotations.NotNull;

public interface CodecRegistry extends Registry<Codec> {

  byte @NotNull [] encode(Object object);

  Object decode(byte[] bytes);

  <T> T decode(byte[] bytes, Class<T> clazz);
}
