package net.aincraft.boost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.aincraft.api.container.Codec;
import net.aincraft.api.registry.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface CodecLoader {

  default void load(Registry<Codec> codecRegistry) {
    codecs().forEach(codecRegistry::register);
  }

  Collection<Codec> codecs();
}
