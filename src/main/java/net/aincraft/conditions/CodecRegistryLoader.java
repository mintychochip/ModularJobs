package net.aincraft.conditions;

import java.util.ArrayList;
import java.util.List;
import net.aincraft.api.container.boost.Condition.Codec;
import net.aincraft.api.registry.Registry;
import net.aincraft.conditions.PlayerResourceConditionImpl.CodecImpl;

public final class CodecRegistryLoader {

  private final List<Codec> codecList = new ArrayList<>();

  public static final CodecRegistryLoader INSTANCE = new CodecRegistryLoader();

  private CodecRegistryLoader() {
    codecList.add(new ComposableConditionImpl.CodecImpl());
    codecList.add(new SneakConditionImpl.CodecImpl());
    codecList.add(new SprintConditionImpl.CodecImpl());
    codecList.add(new WorldConditionImpl.CodecImpl());
    codecList.add(new NegatingConditionImpl.CodecImpl());
    codecList.add(new WorldConditionImpl.CodecImpl());
    codecList.add(new BiomeConditionImpl.CodecImpl());
    codecList.add(new CodecImpl());
  }

  public void load(Registry<Codec> codecRegistry) {
    codecList.forEach(codecRegistry::register);
  }

}
