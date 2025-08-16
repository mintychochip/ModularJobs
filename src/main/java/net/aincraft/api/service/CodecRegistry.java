package net.aincraft.api.service;

import net.aincraft.api.container.Codec;
import net.aincraft.api.registry.Registry;

public interface CodecRegistry extends Registry<Codec> {

  byte[] encode(Object object);

  Object decode(byte[] bytes);

  <T> T decode(byte[] bytes, Class<T> clazz);
}
