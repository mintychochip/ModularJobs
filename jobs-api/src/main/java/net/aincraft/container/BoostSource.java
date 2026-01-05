package net.aincraft.container;

import java.util.List;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A boostSource of one or more {@link Boost Boosts}.
 * <p>
 * Each boostSource defines its own semantics:
 * <ul>
 *   <li>What type of boosts it produces,</li>
 *   <li>When they apply for a given {@link BoostContext},</li>
 *   <li>And how many to return.</li>
 * </ul>
 * A boostSource may yield none, one, or many boosts. It is the
 * responsibility of the implementation to determine the
 * behavior behind its output.
 */
public interface BoostSource extends Keyed {

  /**
   * Evaluate this boostSource for the given context.
   *
   * @param context the current evaluation context
   * @return a list of boosts from this boostSource (never {@code null})
   */
  @NotNull
  List<Boost> evaluate(BoostContext context);

  /**
   * Get the description of this boost source.
   *
   * @return description or null if not provided
   */
  @Nullable
  String description();

}
