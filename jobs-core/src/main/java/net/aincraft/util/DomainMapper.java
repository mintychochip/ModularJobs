package net.aincraft.util;

import org.jetbrains.annotations.NotNull;

public interface DomainMapper<D, R> {

  @NotNull
  D toDomainObject(@NotNull R record) throws IllegalArgumentException;

}
