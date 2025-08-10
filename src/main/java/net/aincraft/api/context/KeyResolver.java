package net.aincraft.api.context;

import net.aincraft.api.Bridge;
import net.kyori.adventure.key.Key;

public interface KeyResolver {

  static KeyResolver keyResolver() {
    return Bridge.bridge().resolver();
  }

  Key resolve(Context object);

  <T extends Context> void addStrategy(KeyResolvingStrategy<T> strategy);

  interface KeyResolvingStrategy<T extends Context> {

    Class<T> getType();

    Key resolve(T object);
  }
}
