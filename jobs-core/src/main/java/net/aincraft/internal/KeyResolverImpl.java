package net.aincraft.internal;

import java.util.HashMap;
import java.util.Map;
import net.aincraft.container.Context;
import net.aincraft.container.KeyResolver;
import net.kyori.adventure.key.Key;

public final class KeyResolverImpl implements KeyResolver {

  private final Map<Class<? extends Context>, KeyResolvingStrategy<?>> strategies = new HashMap<>();

  @Override
  public Key resolve(Context context) {
    Class<? extends Context> objectClass = context.getClass();
    KeyResolvingStrategy<?> raw = strategies.get(objectClass);
    if (raw == null) {
      return null;
    }
    return resolve(raw, context);
  }

  @Override
  public <T extends Context> void addStrategy(Class<T> clazz, KeyResolvingStrategy<T> strategy) {
    strategies.put(clazz,strategy);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Context> Key resolve(KeyResolvingStrategy<?> raw, Context object) {
    KeyResolvingStrategy<T> strategy = (KeyResolvingStrategy<T>) raw;
    T casted = (T) object;
    return strategy.resolve(casted);
  }

}
