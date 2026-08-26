package dev.mintychochip.upgrade;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A single purchasable level of a skill node.
 *
 * @param cost skill points required to buy this level
 * @param effects effects granted by reaching this level (see {@link SkillNode#activeEffects})
 */
public record NodeLevel(int cost, @NotNull List<NodeEffect> effects) {
  /** API member. */
  public NodeLevel {
    if (cost < 0) {
      throw new IllegalArgumentException("Node level cost must be non-negative");
    }
    effects = List.copyOf(effects);
  }
}
