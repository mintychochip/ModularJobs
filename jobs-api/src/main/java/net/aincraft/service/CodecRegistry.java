package net.aincraft.service;

import net.aincraft.container.Codec;
import net.aincraft.registry.Registry;

public interface CodecRegistry extends Registry<Codec> {

  byte[] encode(Object object);

  Object decode(byte[] bytes);

  <T> T decode(byte[] bytes, Class<T> clazz);
}
