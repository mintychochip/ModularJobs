package net.aincraft.util;

import net.aincraft.container.Context;
import net.kyori.adventure.key.Key;

public interface KeyResolver {

  Key resolve(Context context);

  <T extends Context> void addStrategy(Class<T> clazz, KeyResolvingStrategy<T> strategy);

  interface KeyResolvingStrategy<T extends Context> {

    Key resolve(T object);
  }
}
