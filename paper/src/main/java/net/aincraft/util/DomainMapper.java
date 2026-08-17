package net.aincraft.util;

import org.jetbrains.annotations.NotNull;

/**
 * Converts between a domain value and its persistence representation.
 *
 * @param <D> domain type
 * @param <R> record/persistence type
 */
public interface DomainMapper<D, R> {

  /**
   * Reconstructs a domain value from a record.
   *
   * @param record persisted record; must not be {@code null}
   * @return domain value
   * @throws IllegalArgumentException if the record cannot be converted
   */
  @NotNull
  D toDomain(@NotNull R record) throws IllegalArgumentException;

  /**
   * Converts a domain value to a persistence record.
   *
   * @param domain domain value; must not be {@code null}
   * @return persistence record
   */
  @NotNull
  R toRecord(@NotNull D domain);
}
