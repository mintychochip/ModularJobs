package net.aincraft.boost;

import java.util.Collection;
import net.aincraft.container.Codec;
import net.aincraft.registry.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface CodecLoader {

  default void load(Registry<Codec> codecRegistry) {
    codecs().forEach(codecRegistry::register);
  }

  Collection<Codec> codecs();
}
