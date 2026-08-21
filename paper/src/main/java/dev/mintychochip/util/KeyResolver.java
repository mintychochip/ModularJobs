package dev.mintychochip.util;

import java.util.HashMap;
import java.util.Map;
import dev.mintychochip.container.Context;
import net.kyori.adventure.key.Key;

/**
 * Resolves context objects to stable keys used by task persistence.
 *
 * <p>Strategies are matched by the context's exact runtime class. Missing
 * strategies return {@code null} rather than throwing.
 */
public final class KeyResolver {

  private final Map<Class<? extends Context>, KeyResolvingStrategy<?>> strategies = new HashMap<>();

  /**
   * Resolves a context using the strategy registered for its exact class.
   *
   * @param context context to resolve
   * @return resolved key, or {@code null} when no strategy is registered
   */
  public Key resolve(Context context) {
    Class<? extends Context> objectClass = context.getClass();
    KeyResolvingStrategy<?> raw = strategies.get(objectClass);
    if (raw == null) {
      return null;
    }
    return resolve(raw, context);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Context> Key resolve(KeyResolvingStrategy<?> raw, Context object) {
    KeyResolvingStrategy<T> strategy = (KeyResolvingStrategy<T>) raw;
    T casted = (T) object;
    return strategy.resolve(casted);
  }

  /**
   * Registers or replaces a strategy for a context class.
   *
   * @param clazz exact context class handled by the strategy
   * @param strategy resolver invoked for matching contexts
   */
  public <T extends Context> void addStrategy(Class<T> clazz, KeyResolvingStrategy<T> strategy) {
    strategies.put(clazz, strategy);
  }

  /**
   * Strategy for converting one context type into a stable lookup key.
   *
   * @param <T> context type accepted by this strategy
   */
  @FunctionalInterface
  public interface KeyResolvingStrategy<T extends Context> {

    /**
     * Resolves a context into its persistence key.
     *
     * @param object context instance
     * @return key, or {@code null} when it cannot be resolved
     */
    Key resolve(T object);
  }
}
