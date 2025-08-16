package net.aincraft.api.service;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.Condition.Codec;
import net.aincraft.api.registry.Registry;

public interface CodecRegistry extends Registry<Codec> {

  byte[] encode(Condition condition);

  Condition decode(byte[] bytes);
}
