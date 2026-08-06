package net.aincraft.util;

import java.util.HashMap;
import java.util.Map;
import net.aincraft.container.Context;
import net.kyori.adventure.key.Key;

public final class KeyResolver {

  private final Map<Class<? extends Context>, KeyResolvingStrategy<?>> strategies = new HashMap<>();

  public Key resolve(Context context) {
    Class<? extends Context> objectClass = context.getClass();
    KeyResolvingStrategy<?> raw = strategies.get(objectClass);
    if (raw == null) {
      return null;
    }
    return resolve(raw, context);
  }

  public <T extends Context> void addStrategy(Class<T> clazz, KeyResolvingStrategy<T> strategy) {
    strategies.put(clazz,strategy);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Context> Key resolve(KeyResolvingStrategy<?> raw, Context object) {
    KeyResolvingStrategy<T> strategy = (KeyResolvingStrategy<T>) raw;
    T casted = (T) object;
    return strategy.resolve(casted);
  }



  public interface KeyResolvingStrategy<T extends Context> {

    Key resolve(T object);
  }
}
