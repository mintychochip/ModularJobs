package net.aincraft.util;

import org.jetbrains.annotations.NotNull;

public interface Mapper<D, R> {

  @NotNull
  D toDomainObject(@NotNull R record) throws IllegalArgumentException;

}
