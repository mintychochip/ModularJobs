package net.aincraft.bridge;

import java.util.HashMap;
import java.util.Map;
import net.aincraft.api.context.Context;
import net.aincraft.api.context.KeyResolver;
import net.kyori.adventure.key.Key;

public final class KeyResolverImpl implements KeyResolver {

  private final Map<Class<? extends Context>, KeyResolvingStrategy<?>> strategies = new HashMap<>();

  @Override
  public Key resolve(Context object) {
    Class<? extends Context> objectClass = object.getClass();
    KeyResolvingStrategy<?> raw = strategies.get(objectClass);
    if (raw == null) {
      return null;
    }
    return resolve(raw, object);
  }

  @Override
  public <T extends Context> void addStrategy(KeyResolvingStrategy<T> strategy) {
    strategies.put(strategy.getType(), strategy);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Context> Key resolve(KeyResolvingStrategy<?> raw, Context object) {
    KeyResolvingStrategy<T> strategy = (KeyResolvingStrategy<T>) raw;
    T casted = (T) object;
    return strategy.resolve(casted);
  }

}
