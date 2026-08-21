package dev.mintychochip.upgrade;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * A keyed state write performed when a major node is purchased. keys are namespaced (e.g. {@code
 * tree.vocation}).
 *
 * @param op SET writes {@code key = value}; REMOVE clears the key
 * @param key namespaced state key
 * @param value value for SET; ignored for REMOVE
 */
public record NodeStateWrite(@NotNull Op op, @NotNull Key key, @NotNull String value) {
  /** Op. */
  public enum Op {
    SET,
    REMOVE
  }
}
