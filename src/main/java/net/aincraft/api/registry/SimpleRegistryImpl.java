package net.aincraft.api.registry;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

final class SimpleRegistryImpl<T extends Keyed> implements Registry<T> {

  private final Map<Key, T> registry = new HashMap<>();

  @Override
  public @NotNull T getOrThrow(Key key) throws IllegalArgumentException {
    Preconditions.checkArgument(isRegistered(key));
    return registry.get(key);
  }

  @Override
  public boolean isRegistered(Key key) {
    return registry.containsKey(key);
  }

  @Override
  public void register(@NotNull T object) {
    registry.put(object.key(),object);
  }
}