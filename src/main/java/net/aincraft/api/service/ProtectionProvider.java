package net.aincraft.api.service;

import java.time.temporal.TemporalAmount;
import org.jetbrains.annotations.NotNull;

public interface ProtectionProvider<T> {
  @NotNull
  TemporalAmount getTotalProtectionDuration(T object) throws IllegalArgumentException;
  boolean canProtect(T object);
  void removeProtection(T object);
  void addProtection(T object) throws IllegalArgumentException;
  boolean isProtected(T object);
  TemporalAmount getRemainingProtectionTime(T object);
}
