package net.aincraft.service;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.aincraft.api.service.ProtectionProvider;
import org.jetbrains.annotations.NotNull;

public final class MemoryProtectionProviderImpl<T, V> implements ProtectionProvider<T> {

  private final Map<V, TemporalAmount> protectionDurations;
  private final Map<Object, Instant> protectionTimers = new HashMap<>();
  private final Function<T, V> categoryKeyFn;
  private final LoadingCache<T, Object> identityCache;

  public MemoryProtectionProviderImpl(
      Map<V, TemporalAmount> protectionDurations,
      Function<T, V> categoryKeyFn,
      CacheLoader<T, Object> identityLoader
  ) {
    this.protectionDurations = protectionDurations;
    this.categoryKeyFn = categoryKeyFn;
    this.identityCache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(10)).build(identityLoader);
  }

  @Override
  public boolean canProtect(T object) {
    return protectionDurations.containsKey(categoryKeyFn.apply(object));
  }

  @Override
  public @NotNull TemporalAmount getTotalProtectionDuration(T object) {
    Preconditions.checkArgument(canProtect(object));
    return protectionDurations.get(categoryKeyFn.apply(object));
  }

  @Override
  public void addProtection(T object) throws IllegalArgumentException {
    Preconditions.checkArgument(canProtect(object));
    Object key = identityCache.getUnchecked(object);
    TemporalAmount duration = protectionDurations.get(categoryKeyFn.apply(object));
    Instant expiry = Instant.now().plus(duration);
    protectionTimers.put(key, expiry);
  }

  @Override
  public void removeProtection(T object) {
    Object key = identityCache.getUnchecked(object);
    protectionTimers.remove(key);
  }

  @Override
  public boolean isProtected(T object) {
    Object key = identityCache.getUnchecked(object);
    Instant expiry = protectionTimers.get(key);
    if (expiry == null || expiry.isBefore(Instant.now())) {
      protectionTimers.remove(key);
      return false;
    }
    return true;
  }

  @Override
  public @NotNull TemporalAmount getRemainingProtectionTime(T object) {
    Object key = identityCache.getUnchecked(object);
    Instant expiry = protectionTimers.get(key);
    if (expiry == null || expiry.isBefore(Instant.now())) {
      protectionTimers.remove(key);
      return Duration.ZERO;
    }
    return Duration.between(Instant.now(), expiry);
  }
}
