package net.aincraft.boost;

import java.util.Collection;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.registry.Registry;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface CodecLoader {

  default void load(Registry<Codec> codecRegistry) {
    codecs().forEach(codecRegistry::register);
  }

  Collection<Codec> codecs();
}
